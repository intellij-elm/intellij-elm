package org.elm.lang.core.types

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.diagnostics.RedefinitionError
import org.elm.lang.core.diagnostics.TooManyArgumentsError
import org.elm.lang.core.diagnostics.TypeMismatchError
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.scope.ExpressionScope

fun ElmValueDeclaration.inference(): InferenceResult { // TODO cache
    val visibleNames = ExpressionScope(parent).getVisibleValues().mapNotNullTo(HashSet()) { it.name }
    declaredNames(false).mapNotNullTo(visibleNames) { it.name }
    return InferenceScope(visibleNames, null).infer(this)
}

/**
 * @property visibleNames names of declared elements visible to elements declared in this scope
 */
private class InferenceScope(
        private val visibleNames: MutableSet<String>,
        parent: InferenceScope?
) {
    // cache for declared tys referenced in this scope; shared with parent
    private val resolvedDeclarations: MutableMap<ElmValueDeclaration, Ty> = parent?.resolvedDeclarations
            ?: mutableMapOf()
    // names declared in patterns; copied from parent since we can see parent's bindings, but ours
    // shouldn't be shared with other scopes that share a parent.
    private val bindings: MutableMap<ElmNamedElement, Ty> = parent?.bindings?.toMutableMap() ?: mutableMapOf()
    // errors encountered during inference
    private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf()

    fun infer(valueDeclaration: ElmValueDeclaration): InferenceResult {
        val declaredTy = bindParameters(valueDeclaration)

        val expr = valueDeclaration.expression
        if (expr != null) {
            val bodyTy = inferType(expr)
            val expected = (declaredTy as? TyFunction)?.ret ?: declaredTy
            requireAssignable(expr, bodyTy, expected)
        }
        return InferenceResult(bindings, diagnostics, declaredTy)
    }

    private fun infer(lambda: ElmAnonymousFunction): InferenceResult {
        // TODO [unification] infer param types
        lambda.namedParameters.forEach { setBinding(it, TyUnknown) }
        val bodyTy = inferType(lambda.expression)
        return InferenceResult(bindings, diagnostics, bodyTy)
    }

    fun setBinding(element: ElmNamedElement, ty: Ty) {
        val elementName = element.name
        if (elementName in visibleNames) {
            diagnostics += RedefinitionError(element)
        } else {
            bindings[element] = ty
            if (elementName != null) visibleNames += elementName
        }
    }

    /** @return the entire declared type, or [TyUnknown] if no annotation exists */
    private fun bindParameters(valueDeclaration: ElmValueDeclaration): Ty {
        // TODO [drop 0.18] remove this if
        if (valueDeclaration.pattern != null || valueDeclaration.operatorDeclarationLeft != null) {
            // this is 0.18 only, so we aren't going to bother implementing it
            bindings += PsiTreeUtil.collectElementsOfType(valueDeclaration.pattern, ElmLowerPattern::class.java)
                    .associate { it to TyUnknown }
            return TyUnknown
        }

        val decl = valueDeclaration.functionDeclarationLeft!!
        val typeRef = valueDeclaration.typeAnnotation?.typeRef

        if (typeRef == null) {
            decl.namedParameters.forEach { setBinding(it, TyUnknown) }
            return TyUnknown
        }

        val types = typeRef.allParameters
        decl.patterns.zip(types).forEach { (pat, type) -> bindPattern(this, pat, type.ty) }
        return typeRef.ty
    }

    private fun inferType(expr: ElmExpression): Ty {
        val parts = expr.parts.map { part ->
            when (part) {
                is ElmOperator -> {
                    TyUnknown
                } // TODO operators
                is ElmOperandTag -> {
                    inferType(part)
                }
                else -> TyUnknown
            }
        }

        // TODO operators
        return parts.singleOrNull() ?: TyUnknown
    }

    private fun inferType(operand: ElmOperandTag): Ty {
        return when (operand) {
            is ElmAnonymousFunction -> {
                val inferenceResult = InferenceScope(visibleNames.toMutableSet(), this).infer(operand)
                diagnostics += inferenceResult.diagnostics
                inferenceResult.ty
            }
            is ElmCaseOf -> TyUnknown // TODO implement
            is ElmFieldAccess -> TyUnknown // TODO we need to get the record type from somewhere
            is ElmFunctionCall -> inferType(operand)
            is ElmIfElse -> inferType(operand)
            is ElmLetIn -> TyUnknown // TODO implement
            is ElmList -> TyList(operand.expressionList.map { inferType(it) }.firstOrNull()
                    ?: TyUnknown)  // TODO check and unify elements
            is ElmCharConstant -> TyChar
            is ElmStringConstant -> TyString
            is ElmNumberConstant -> {
                if (operand.isFloat) TyFloat
                else TyUnknown  // TODO int literals have type `numberN`, and we need to infer them
            }
            is ElmNegateExpression -> operand.expression?.let { inferType(it) } ?: TyUnknown
            is ElmNonEmptyTuple -> TyTuple(operand.expressionList.map { inferType(it) })
            is ElmOperatorAsFunction -> inferType(operand)
            is ElmRecord -> {
                if (operand.baseRecordIdentifier != null) TyUnknown // TODO the type is the type of the base record
                else TyRecord(operand.fieldList.associate { f ->
                    f.lowerCaseIdentifier.text to (f.expression?.let { inferType(it) } ?: TyUnknown)
                })
            }
            is ElmValueExpr -> inferType(operand)
            is ElmTupleConstructor -> TyUnknown // TODO [drop 0.18] remove this case
            is ElmUnit -> TyUnit
            is ElmExpression -> inferType(operand) // parenthesized expression
            is ElmGlslCode -> TyShader
            else -> error("unexpected operand type $operand")
        }
    }

    private fun inferType(expr: ElmIfElse): Ty {
        val expressionList = expr.expressionList
        if (expressionList.size < 3 || expressionList.size % 2 == 0) return TyUnknown // incomplete program
        val expressionTys = expressionList.map { inferType(it) }

        // check all the conditions
        for (i in 0 until expressionList.lastIndex step 2) {
            requireAssignable(expressionList[i], expressionTys[i], TyBool)
        }

        // check that all branches match the first
        for (i in expressionList.indices) {
            if (i != expressionList.lastIndex && (i < 3 || i % 2 == 0)) continue

            if (!requireAssignable(expressionList[i], expressionTys[i], expressionTys[1])) {
                // Only issue an error on the first mismatched branch to avoid spam in the case the
                // only the first branch is different
                break
            }
        }

        return expressionTys[1]
    }

    private fun inferType(expr: ElmValueExpr): Ty {
        val ref = expr.reference.resolve() ?: return TyUnknown

        // If the value is a parameter, its type has already been added to bindings
        bindings[ref]?.let { return it }

        if (ref is ElmUnionMember) {
            val decl = ref.parentOfType<ElmTypeDeclaration>()?.ty ?: return TyUnknown
            val params = ref.allParameters.map { it.ty }.toList()

            return if (params.isNotEmpty()) {
                // Constructors with parameters are functions returning the type.
                TyFunction(params, decl)
            } else {
                // Constructors without parameters are just instances of the type, since there are no nullary functions.
                decl
            }
        } else {
            val decl = ref.parentOfType<ElmValueDeclaration>() ?: return TyUnknown
            return inferType(decl)
        }

    }

    private fun inferType(call: ElmFunctionCall): Ty {
        val targetTy = inferType(call.target) // uses the operand tag overload

        val arguments = call.arguments.toList()
        val paramTys = if (targetTy is TyFunction) targetTy.parameters else listOf()

        if (arguments.size > paramTys.size) {
            diagnostics.add(TooManyArgumentsError(call, arguments.size, paramTys.size))
        }

        if (targetTy !is TyFunction) return TyUnknown

        for ((arg, paramTy) in arguments.zip(paramTys)) {
            val argTy = inferType(arg)
            requireAssignable(arg, argTy, paramTy)
        }

        return when {
            // partial application, return a function
            arguments.size < paramTys.size -> TyFunction(paramTys.drop(arguments.size), targetTy.ret)
            else -> targetTy.ret
        }
    }

    private fun inferType(op: ElmOperatorAsFunction): Ty {
        var ref = op.reference.resolve()
        // For operators, we need to resolve the infix declaration to the actual function
        if (ref is ElmInfixDeclaration) {
            ref = ref.valueExpr?.reference?.resolve()
        }
        val decl = ref?.parentOfType<ElmValueDeclaration>() ?: return TyUnknown
        return inferType(decl)
    }

    private fun inferType(decl: ElmValueDeclaration): Ty {
        val existing = resolvedDeclarations[decl]
        if (existing != null) return existing
        // Use the type annotation if there is one, otherwise just count the parameters
        var ty = decl.typeAnnotation?.typeRef?.ty
        if (ty == null) {
            // If there's no annotation, just set all the parameters to unknown for now
            // TODO use the inference
            ty = decl.functionDeclarationLeft?.patterns
                    ?.map { TyUnknown }?.toList()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { TyFunction(it, TyUnknown) }
        }
        if (ty == null) {
            return TyUnknown
        }
        resolvedDeclarations[decl] = ty
        return ty
    }

    private fun requireAssignable(element: PsiElement, ty1: Ty, ty2: Ty): Boolean {
        val assignable = assignable(ty1, ty2)
        if (!assignable) {
            diagnostics.add(TypeMismatchError(element, ty1, ty2))
        }
        return assignable
    }

    /** Return `false` if [ty1] definitely cannot be assigned to [ty2] */
    private fun assignable(ty1: Ty, ty2: Ty): Boolean {
        // TODO inference for vars
        return ty1 === ty2 || ty2 is TyVar || ty2 is TyUnknown || when (ty1) {
            is TyVar -> true
            is TyTuple -> ty2 is TyTuple
                    && ty1.types.size == ty2.types.size
                    && allAssignable(ty1.types, ty2.types)
            is TyRecord -> ty2 is TyRecord
                    && ty1.fields.size == ty2.fields.size
                    && ty1.fields.all { (k, v) -> ty2.fields[k]?.let { assignable(v, it) } ?: false }
            is TyUnion -> ty2 is TyUnion
                    && ty1.name == ty2.name
                    && ty1.module == ty2.module
                    && allAssignable(ty1.parameters, ty2.parameters)
            is TyFunction -> ty2 is TyFunction
                    && allAssignable(ty1.allTys, ty2.allTys)
            // object tys are covered by the identity check above
            TyShader, TyUnit -> false
            TyUnknown -> true
        }
    }

    private fun allAssignable(ty1: List<Ty>, ty2: List<Ty>) = ty1.zip(ty2).all { (l, r) -> assignable(l, r) }
}


/**
 * @property ty the return type of the function or expression being inferred
 */
data class InferenceResult(private val bindings: Map<ElmNamedElement, Ty>,
                           val diagnostics: List<ElmDiagnostic>,
                           val ty: Ty) {
    fun bindingType(element: ElmNamedElement): Ty = bindings[element] ?: TyUnknown
}

private fun bindPattern(ctx: InferenceScope, pat: ElmFunctionParamOrPatternChildTag, ty: Ty) {
    return when (pat) {
        is ElmAnythingPattern -> {
        }
        is ElmConsPattern -> {
        } // TODO This is a partial pattern error in function parameters
        is ElmListPattern -> {
        } // This is a partial pattern error in function parameters
        is ElmConstantTag -> {
        } // This is a partial pattern error in function parameters
        is ElmPattern -> {
            bindPattern(ctx, pat.child, ty)
            bindPattern(ctx, pat.patternAs, ty)
        }
        is ElmLowerPattern -> ctx.setBinding(pat, ty)
        is ElmRecordPattern -> bindPattern(ctx, pat, ty)
        is ElmTuplePattern -> bindPattern(ctx, pat, ty)
        is ElmUnionPattern -> {
            // TODO this can appear in function params if there is only one union case (i.e. a wrapper)
        }
        is ElmUnit -> {
        }
        else -> error("unexpected type $pat")
    }
}

private fun bindPattern(ctx: InferenceScope, pat: ElmPatternAs?, ty: Ty) {
    if (pat != null) ctx.setBinding(pat, ty)
}

private fun bindPattern(ctx: InferenceScope, pat: ElmTuplePattern, ty: Ty) {
    if (ty !is TyTuple) return // TODO: report error
    pat.patternList
            .zip(ty.types)
            .forEach { (pat, type) -> bindPattern(ctx, pat.child, type) }
}

private fun bindPattern(ctx: InferenceScope, pat: ElmRecordPattern, ty: Ty) {
    if (ty !is TyRecord) return // TODO: report error
    for (id in pat.lowerPatternList) {
        val fieldTy = ty.fields[id.name] ?: continue // TODO: report error
        bindPattern(ctx, id, fieldTy)
    }
}

/** Get the type for one part of a type ref */
private val ElmTypeSignatureDeclarationTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> ty
        is ElmTypeVariableRef -> TyVar(identifier.text) // TODO
        is ElmRecordType -> TyRecord(fieldTypeList.associate { it.lowerCaseIdentifier.text to it.typeRef.ty })
        is ElmTupleType -> if (unit != null) TyUnit else TyTuple(typeRefList.map { it.ty })
        is ElmParametricTypeRef -> ty
        is ElmTypeRef -> ty
        else -> error("unimplemented type $this")
    }

private val ElmParametricTypeRef.ty: TyUnion
    get() {
        val ref = reference.resolve()
        val parameters = allParameters.map { it.ty }.toList()
        val name = ref?.name ?: upperCaseQID.text
        val module = ref?.moduleName ?: builtInModule(name) ?: moduleName
        return TyUnion(module, name, parameters)
    }

private val ElmPsiElement.moduleName: String
    get() = elmFile.getModuleDecl()?.name ?: ""

private val ElmUpperPathTypeRef.ty: Ty
    get() {
        val ref = reference.resolve()

        return when (ref) {
            // If this is referencing an alias, use aliased type
            is ElmTypeAliasDeclaration -> ref.typeRef?.ty ?: TyUnknown
            is ElmTypeDeclaration -> ref.ty
            else -> {
                val name = ref?.name ?: text
                val module = ref?.moduleName ?: builtInModule(name) ?: moduleName
                TyUnion(module, name, emptyList())
            }
        }
    }

private val ElmTypeRef.ty: Ty
    get() {
        val params = allParameters.toList()
        return when {
            params.size == 1 -> params[0].ty
            else -> TyFunction(params.dropLast(1).map { it.ty }, params.last().ty)
        }
    }

private val ElmTypeDeclaration.ty: Ty
    get() = TyUnion(moduleName, name, lowerTypeNameList.map { TyVar(it.name) })

/** Return the module name for built-in types, or null */
private fun builtInModule(name: String): String? {
    return when (name) {
        "Int", "Float" -> "Basics"
        "String" -> "String"
        "Char" -> "Char"
        "List" -> "List"
        else -> null
    }
}
