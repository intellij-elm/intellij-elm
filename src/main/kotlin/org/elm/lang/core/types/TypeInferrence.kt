package org.elm.lang.core.types

import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.diagnostics.TooManyArgumentsError
import org.elm.lang.core.diagnostics.TypeMismatchError
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

val ElmValueDeclaration.inference: InferenceResult
    get () { // TODO cache
        return InferenceContext().infer(this)
    }

private class InferenceContext {
    val bindings: MutableMap<ElmNamedElement, Ty> = mutableMapOf()
    val operandTypes: MutableMap<ElmOperandTag, Ty> = mutableMapOf()
    val resolvedDecls: MutableMap<ElmValueDeclaration, Ty> = mutableMapOf()
    val diagnostics: MutableList<ElmDiagnostic> = mutableListOf()

    /**
     * The type of this declaration.
     *
     * Usually a [TyFunction], but could be any type for plain value assignments like `x = 1`.
     */
    var ty: Ty = TyUnknown
        private set

    fun infer(valueDeclaration: ElmValueDeclaration): InferenceResult {
        bindParameters(valueDeclaration)

        val expr = valueDeclaration.expression
        if (expr != null) {
            val ret = inferType(expr)
            val expected = (ty as? TyFunction)?.ret ?: ty
            if (!assignable(ret, expected)) {
                diagnostics.add(TypeMismatchError(expr, ret, expected))
            }
        }
        return InferenceResult(bindings, operandTypes, resolvedDecls, diagnostics)
    }

    fun bindParameters(valueDeclaration: ElmValueDeclaration) {
        // TODO [drop 0.18] remove this if
        if (valueDeclaration.pattern != null || valueDeclaration.operatorDeclarationLeft != null) {
            // this is 0.18 only, so we aren't going to bother implementing it
            bindings += PsiTreeUtil.collectElementsOfType(valueDeclaration.pattern, ElmLowerPattern::class.java)
                    .associate { it to TyUnknown }
            return
        }

        val decl = valueDeclaration.functionDeclarationLeft!!
        val typeRef = valueDeclaration.typeAnnotation?.typeRef
        val types = typeRef?.allParameters

        if (typeRef == null || types == null) {
            bindings += decl.namedParameters.associate { it to TyUnknown }
            return
        }

        ty = typeRef.ty
        decl.patterns.zip(types).forEach { (pat, type) -> bindPattern(this, pat, type.ty) }
    }

    fun inferType(expr: ElmExpression): Ty {
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

    fun inferType(operand: ElmOperandTag): Ty {
        return when (operand) {
            is ElmAnonymousFunction -> {
                // TODO infer params types
                TyFunction(operand.patternList.map { TyUnknown }, inferType(operand.expression))
            }
            is ElmCaseOf -> TyUnknown // TODO
            is ElmFieldAccess -> TyUnknown // TODO we need to get the record type from somewhere
            is ElmFunctionCall -> inferType(operand)
            is ElmGlslCode -> TyShader
            is ElmIfElse -> TyUnknown // TODO
            is ElmLetIn -> TyUnknown // TODO
            is ElmList -> TyList(operand.expressionList.map { inferType(it) })
            is ElmLiteral -> when {
                operand.isChar -> TyChar
                operand.isString -> TyString
                operand.textContains('.') -> TyFloat
                else -> TyUnknown // TODO int literals have type `number`, and we need to infer them
            }
            is ElmNegateExpression -> operand.expression?.let { inferType(it) } ?: TyUnknown
            is ElmNonEmptyTuple -> TyTuple(operand.expressionList.map { inferType(it) })
            is ElmOperatorAsFunction -> getDeclTy(operand) ?: TyUnknown
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
            else -> error("unexpected operand type $operand")
        }
    }

    fun inferType(expr: ElmValueExpr): Ty {
        val ref = expr.reference.resolve() ?: return TyUnknown
        if (ref in bindings) return bindings[ref]!!
        val member = ref as? ElmUnionMember ?: return TyUnknown
        val decl = member.parentOfType<ElmTypeDeclaration>()?.ty ?: return TyUnknown
        val params = member.allParameters.map { it.ty }.toList()

        return if (params.isNotEmpty()) {
            // Constructors with parameters are functions returning the type.
            TyFunction(params, decl)
        } else {
            // Constructors without parameters are just instances of the type, since there are no nullary functions.
            decl
        }
    }

    fun inferType(call: ElmFunctionCall): Ty {
        val declTy = when {
            call.function != null -> getDeclTy(call.function!!)
            call.operator != null -> getDeclTy(call.operator!!)
            else -> null
        } ?: return TyUnknown // TODO use inference

        val arguments = call.arguments.toList()
        val paramTys = if (declTy is TyFunction) declTy.parameters else listOf(declTy)

        if (arguments.size > paramTys.size) {
            diagnostics.add(TooManyArgumentsError(call, arguments.size, paramTys.size))
        }

        for ((arg, paramTy) in arguments.zip(paramTys)) {
            val argTy = inferType(arg)
            if (!assignable(argTy, paramTy)) {
                diagnostics.add(TypeMismatchError(arg, argTy, paramTy))
            }
        }

        return when (declTy) {
            is TyFunction ->
                when {
                    // partial application, return a function
                    arguments.size < paramTys.size -> TyFunction(paramTys.drop(arguments.size), declTy.ret)
                    else -> declTy.ret
                }
            else -> declTy
        }
    }

    private fun getDeclTy(op: ElmOperatorAsFunction): Ty? {
        var ref = op.reference.resolve()
        // For operators, we need to resolve the infix declaration to the actual function
        if (ref is ElmInfixDeclaration) {
            ref = ref.valueExpr?.reference?.resolve()
        }
        val decl = ref?.parentOfType<ElmValueDeclaration>()
                ?: return null
        return getDeclTy(decl)
    }

    private fun getDeclTy(name: ElmValueExpr): Ty? {
        val ref = name.reference.resolve()
        val decl = ref?.parentOfType<ElmValueDeclaration>()
                ?: return null
        return getDeclTy(decl)
    }

    private fun getDeclTy(decl: ElmValueDeclaration): Ty? {
        val existing = resolvedDecls[decl]
        if (existing != null) return existing
        // Use the type annotation if there is one, otherwise just count the parameters
        // TODO use the inference
        val ty = decl.typeAnnotation?.typeRef?.ty
                ?: decl.functionDeclarationLeft?.patterns
                        ?.map { TyUnknown }?.toList()
                        ?.let { TyFunction(it, TyUnknown) }
                ?: return null
        resolvedDecls[decl] = ty
        return ty
    }

    /** Return false if [ty1] definitely cannot be assigned to [ty2] */
    private fun assignable(ty1: Ty, ty2: Ty): Boolean {
        // TODO inference for vars
        return ty1 === ty2 || ty2 is TyVar || ty2 is TyUnknown || when (ty1) {
            is TyVar -> true
            is TyTuple -> ty2 is TyTuple && ty1.types.size == ty2.types.size
                    && allAssignable(ty1.types, ty2.types)
            is TyRecord -> ty2 is TyRecord && ty1.fields.size == ty2.fields.size
                    && ty1.fields.all { (k, v) -> ty2.fields[k]?.let { assignable(v, it) } ?: false }
            is TyPrimitive -> ty2 is TyPrimitive && ty1.name == ty2.name
            is TyParametric -> ty2 is TyParametric && ty1.name == ty2.name
                    && allAssignable(ty1.parameters, ty2.parameters)
            is TyFunction -> ty2 is TyFunction && allAssignable(ty1.allTys, ty2.allTys)
            // object tys are covered by the identity check above
            TyShader, TyUnit -> false
            TyUnknown -> true
        }
    }

    private fun allAssignable(ty1: List<Ty>, ty2: List<Ty>) = ty1.zip(ty2).all { (l, r) -> assignable(l, r) }
}


data class InferenceResult(private val bindings: Map<ElmNamedElement, Ty>,
                           private val operandTypes: MutableMap<ElmOperandTag, Ty>,
                           private val resolvedDecls: MutableMap<ElmValueDeclaration, Ty>,
                           val diagnostics: List<ElmDiagnostic>) {
    fun bindingType(element: ElmNamedElement): Ty = bindings[element] ?: TyUnknown
}

private fun bindPattern(ctx: InferenceContext, pat: ElmFunctionParamOrPatternChildTag, ty: Ty) {
    return when (pat) {
        is ElmAnythingPattern -> {
        }
        is ElmConsPattern -> {
        } // TODO This is a partial pattern error in function parameters
        is ElmListPattern -> {
        } // This is a partial pattern error in function parameters
        is ElmLiteral -> {
        } // This is a partial pattern error in function parameters
        is ElmPattern -> {
            bindPattern(ctx, pat.child, ty)
            bindPattern(ctx, pat.patternAs, ty)
        }
        is ElmLowerPattern -> ctx.bindings[pat] = ty
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

private fun bindPattern(ctx: InferenceContext, pat: ElmPatternAs?, ty: Ty) {
    if (pat != null) ctx.bindings[pat] = ty
}

private fun bindPattern(ctx: InferenceContext, pat: ElmTuplePattern, ty: Ty) {
    if (ty !is TyTuple) return // TODO: report error
    pat.patternList
            .zip(ty.types)
            .forEach { (pat, type) -> bindPattern(ctx, pat.child, type) }
}

private fun bindPattern(ctx: InferenceContext, pat: ElmRecordPattern, ty: Ty) {
    if (ty !is TyRecord) return // TODO: report error
    for (id in pat.lowerPatternList) {
        val fieldTy = ty.fields[id.name] ?: continue // TODO: report error
        bindPattern(ctx, id, fieldTy)
    }
}

/** Get the type for one part of a type ref */
private val ElmTypeSignatureDeclarationTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> {
            // If this is referencing an aliases, use aliased type
            (reference.resolve() as? ElmTypeAliasDeclaration)?.typeRef?.ty ?: TyPrimitive(text)
        }
        is ElmTypeVariableRef -> TyVar(identifier.text, null) // TODO
        is ElmRecordType -> TyRecord(fieldTypeList.associate { it.lowerCaseIdentifier.text to it.typeRef.ty })
        is ElmTupleType -> if (unit != null) TyUnit else TyTuple(typeRefList.map { it.ty })
        is ElmParametricTypeRef -> TyParametric(upperCaseQID.text, allParameters.map { it.ty }.toList())
        is ElmTypeRef -> ty
        else -> error("unimplemented type $this")
    }

private val ElmTypeRef.ty: Ty
    get() {
        val params = allParameters.toList()
        return when {
            params.size == 1 -> allParameters.first().ty
            else -> TyFunction(params.dropLast(1).map { it.ty }, params.last().ty)
        }
    }

private val ElmTypeDeclaration.ty: Ty
    get() {
        val params = lowerTypeNameList
        return when {
            params.isEmpty() -> TyPrimitive(name)
            else -> TyParametric(name, params.map { TyVar(it.name, null) })
        }
    }
