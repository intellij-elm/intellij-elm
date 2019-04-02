package org.elm.lang.core.types

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue
import com.intellij.psi.util.parentOfType
import org.elm.lang.core.diagnostics.*
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.OperatorAssociativity.NON
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

private val TYPE_INFERENCE_KEY: Key<ParameterizedCachedValue<InferenceResult, Set<ElmValueDeclaration>>> =
        Key.create("TYPE_INFERENCE_KEY")

/** Find the inference result that contains the given element */
fun PsiElement.findInference(): InferenceResult? {
    // the call here is non-strict here so that we can return this element
    return outermostDeclaration(strict = false)?.inference(emptySet())
}

/** Find the type of a given element, if the element is a value expression or declaration */
fun ElmPsiElement.findTy(): Ty? = findInference()?.expressionTypes?.get(this)

private fun ElmValueDeclaration.inference(activeScopes: Set<ElmValueDeclaration>): InferenceResult {
    return CachedValuesManager.getManager(project).getParameterizedCachedValue(this, TYPE_INFERENCE_KEY, { useActiveScopes ->
        // Elm lets you shadow imported names, including auto-imported names, so only count names
        // declared in this file as shadowable.
        val shadowableNames = ModuleScope.getVisibleValues(elmFile).topLevel.mapNotNullTo(mutableSetOf()) { it.name }
        val result = InferenceScope(shadowableNames, useActiveScopes.toMutableSet(), false, null).beginDeclarationInference(this)
        CachedValueProvider.Result.create(result, project.modificationTracker, modificationTracker)
    }, /*trackValue*/ false, /*parameter*/ activeScopes)
}

/**
 * Inference for a single lexical scope (declaration, lambda, or case branch).
 *
 * You can infer a top level declaration by creating an instance of this class and passing the declaration
 * element to [beginDeclarationInference]. This will walk all the branches of the PSI tree, creating nested
 * scopes as needed.
 *
 * @property shadowableNames names of declared elements that will cause a shadowing error if redeclared
 * @property activeScopes scopes that are currently being inferred, to detect invalid recursion; copied from parent
 * @property recursionAllowed if true, diagnostics about self-recursion won't be reported
 */
private class InferenceScope(
        private val shadowableNames: MutableSet<String>,
        private val activeScopes: MutableSet<ElmValueDeclaration>,
        private val recursionAllowed: Boolean,
        private val parent: InferenceScope?
) {
    /**
     * Cache for declared tys referenced in this scope; shared with parent
     * This will cause some items to be cached that aren't visible to all ancestors, but that's fine
     * since you can't read from the cache without a reference to the element anyway, and this way
     * we can cache declarations for sibling elements and other relatives.
     */
    private val resolvedDeclarations: MutableMap<ElmValueDeclaration, Ty> = parent?.resolvedDeclarations
            ?: mutableMapOf()
    /** names declared in parameters and patterns */
    private val bindings: MutableMap<ElmNamedElement, Ty> = mutableMapOf()
    /** errors encountered during inference */
    private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf()
    /**
     * If this scope is a let-in, this set contains declarations that are direct children of this scope.
     *
     * This is used when inferring references to nested declarations that follow the current declaration lexically
     * so that we can find which ancestor to start inference from.
     */
    private val childDeclarations: MutableSet<ElmValueDeclaration> = mutableSetOf()
    /** The inferred types of elements that should be exposed through documentation. */
    private val expressionTypes: MutableMap<ElmPsiElement, Ty> = mutableMapOf()

    /** The unification table used for unannotated parameters */
    private val replacements: DisjointSet = parent?.replacements ?: DisjointSet()

    private val ancestors: Sequence<InferenceScope> get() = generateSequence(this) { it.parent }

    private fun getBinding(e: ElmNamedElement): Ty? = ancestors.mapNotNull { it.bindings[e] }.firstOrNull()

    //<editor-fold desc="entry points">
    /*
     * These functions begin inference for elements that contain lexical scopes. Only one
     * `begin` function should be called on a scope instance.
     */

    fun beginDeclarationInference(declaration: ElmValueDeclaration): InferenceResult {
        val assignee = declaration.assignee

        // If the assignee has any syntax errors, we don't run inference on it. Trying to resolve
        // references in bodies when the parameters contain syntax errors can result in infinite
        // recursion and surprising bugs.
        // We don't currently infer recursive functions in order to avoid infinite loops in the inference.
        if (checkRecursion(declaration) || assignee == null || assignee.hasErrors) {
            return toTopLevelResult(TyUnknown())
        }

        activeScopes += declaration

        // We need to bind parameters before we infer the body since the body can reference the
        // params in functions
        val binding = bindParameters(declaration)

        // The body of pattern declarations is inferred as part of parameter binding, so there's no
        // more to do here.
        if (declaration.pattern != null) {
            return toTopLevelResult(TyUnknown())
        }

        // For function declarations, we need to infer the body and check that it matches the
        // annotation, if there is one.
        val expr = declaration.expression
        var bodyTy: Ty = TyUnknown()
        if (expr != null) {
            bodyTy = inferExpression(expr)

            if (binding is ParameterBindingResult.Annotated) {
                // If the body is just a let expression, show the diagnostic on its expression
                // rather than the whole body.
                val errorExpr = (expr as? ElmLetInExpr)?.expression ?: expr

                val expected = (binding.ty as? TyFunction)?.partiallyApply(binding.count) ?: binding.ty
                requireAssignable(errorExpr, bodyTy, expected)
            }
        }

        val ty = when (binding) {
            is ParameterBindingResult.Annotated -> binding.ty
            is ParameterBindingResult.Unannotated -> {
                if (binding.count == 0) bodyTy
                else TyFunction(binding.params, bodyTy).uncurry()
            }
            is ParameterBindingResult.Other -> bodyTy
        }
        return toTopLevelResult(ty)
    }

    private fun beginLambdaInference(lambda: ElmAnonymousFunctionExpr): InferenceResult {
        val patternList = lambda.patternList
        val paramVars = uniqueVars(patternList.size)
        patternList.zip(paramVars) { p, t -> bindPattern(p, t, true) }
        val bodyTy = inferExpression(lambda.expression)
        return InferenceResult(expressionTypes, diagnostics, TyFunction(paramVars, bodyTy).uncurry())
    }

    private fun beginLetInInference(letIn: ElmLetInExpr): InferenceResult {
        val valueDeclarationList = letIn.valueDeclarationList
        childDeclarations += valueDeclarationList

        for (decl in valueDeclarationList) {
            // If a declaration was referenced by a child defined earlier in this scope, it has
            // already been inferred.
            if (decl !in resolvedDeclarations) {
                inferChildDeclaration(decl)
            }
        }

        val exprTy = inferExpression(letIn.expression)
        return InferenceResult(expressionTypes, diagnostics, exprTy)
    }

    private fun beginCaseBranchInference(
            pattern: ElmPattern,
            caseTy: Ty,
            branchExpression: ElmExpressionTag
    ): InferenceResult {
        bindPattern(pattern, caseTy, false)
        val ty = inferExpression(branchExpression)
        return InferenceResult(expressionTypes, diagnostics, ty)
    }

    private inline fun inferChild(
            activeScopes: MutableSet<ElmValueDeclaration> = this.activeScopes.toMutableSet(),
            recursionAllowed: Boolean = this.recursionAllowed,
            block: InferenceScope.() -> InferenceResult
    ): InferenceResult {
        val result = InferenceScope(shadowableNames.toMutableSet(), activeScopes, recursionAllowed, this).block()
        diagnostics += result.diagnostics
        expressionTypes += result.expressionTypes
        return result
    }

    private fun inferChildDeclaration(decl: ElmValueDeclaration): InferenceResult {
        val result = inferChild { beginDeclarationInference(decl) }
        resolvedDeclarations[decl] = result.ty

        // We need to keep track of declared function names and bound patterns so that other
        // children have access to them.
        val functionName = decl.functionDeclarationLeft?.name ?: decl.operatorDeclarationLeft?.name
        if (functionName != null) {
            shadowableNames += functionName
        } else {
            val pattern = decl.pattern
            if (pattern != null) {
                val patterns = pattern.descendantsOfType<ElmLowerPattern>()
                for (p in patterns) {
                    setBinding(p, result.elementType(p))
                    shadowableNames += p.name
                }
            }
        }
        return result
    }

    private fun checkRecursion(declaration: ElmValueDeclaration): Boolean {
        val isRecursive = declaration in activeScopes
        // Recursion is a compile-time error if the function doesn't have any parameters
        val fdl = declaration.functionDeclarationLeft
        if (isRecursive && !recursionAllowed && (fdl == null || fdl.patterns.firstOrNull() == null)) {
            diagnostics += BadRecursionError(declaration)
        }

        return isRecursive
    }

    // We only call this for inference scopes that will eventually be cached. There's no
    // need to replace everything for child calls since they share our replacements table.
    private fun toTopLevelResult(ty: Ty): InferenceResult {
        val exprs = expressionTypes.mapValues { (_, t) -> TypeReplacement.replace(t, replacements) }
        val ret = TypeReplacement.replace(ty, replacements)
        return InferenceResult(exprs, diagnostics, ret)
    }

    //</editor-fold>
    //<editor-fold desc="inference">
    /*
     * These functions take elements from expressions and return their Ty. If the Ty can't be
     * inferred due to program error or unimplemented functionality, TyUnknown is returned.
     * These functions recurse down into children elements, if any, and report diagnostics on them.
     */

    private fun inferExpression(expr: ElmExpressionTag?): Ty {
        if (expr == null) return TyUnknown()
        return when (expr) {
            is ElmBinOpExpr -> inferBinOpExpr(expr)
            is ElmFunctionCallExpr -> inferFunctionCall(expr)
            is ElmAtomTag -> inferAtom(expr)
            else -> error(expr, "unexpected expression type: $expr")
        }
    }

    private fun inferBinOpExpr(expr: ElmBinOpExpr): Ty {
        if (expr.hasErrors) return TyUnknown()

        val parts: List<ElmBinOpPartTag> = expr.parts.toList()

        // Get the operator types and precedences. We don't have to worry about invalid
        // code like `1 + + 1`, since it won't parse as an expression.
        val operatorPrecedences = HashMap<ElmOperator, OperatorPrecedence>(parts.size / 2)
        val operatorTys = HashMap<ElmOperator, TyFunction>(parts.size / 2)
        var lastPrecedence: OperatorPrecedence? = null
        for (part in parts) {
            if (part is ElmOperator) {
                val (ty, precedence) = inferOperatorAndPrecedence(part)
                when {
                    precedence == null || ty !is TyFunction || ty.parameters.size < 2 -> return TyUnknown()
                    precedence.associativity == NON && lastPrecedence?.associativity == NON -> {
                        // Non-associative operators can't be chained directly with other non-associative
                        // operators.
                        diagnostics += NonAssociativeOperatorError(expr, part)
                        return TyUnknown()
                    }
                    else -> {
                        operatorPrecedences[part] = precedence
                        operatorTys[part] = ty
                    }
                }
                lastPrecedence = precedence
            }
        }

        // Parse the tree and walk it, validating all the operator calls.
        fun validateTree(tree: BinaryExprTree<ElmBinOpPartTag>): TyAndRange {
            return when (tree) {
                is BinaryExprTree.Operand -> {
                    val ty = inferOperand(tree.operand as ElmOperandTag)
                    TyAndRange(tree.operand, ty)
                }
                is BinaryExprTree.Binary -> {
                    val l = validateTree(tree.left)
                    val r = validateTree(tree.right)
                    val func = operatorTys[tree.operator]!!
                    val lAssignable = requireAssignable(l.start, l.ty, func.parameters[0], l.end)
                    val rAssignable = requireAssignable(r.start, r.ty, func.parameters[1], r.end)
                    val ty = when {
                        lAssignable && rAssignable -> TypeReplacement.replace(func.partiallyApply(2), replacements)
                        else -> TyUnknown()
                    }
                    TyAndRange(l.start, r.end, ty)
                }
            }
        }

        val result = validateTree(BinaryExprTree.parse(parts, operatorPrecedences))
        expressionTypes[expr] = result.ty
        return result.ty
    }

    private fun inferOperand(operand: ElmOperandTag): Ty =
            when (operand) {
                is ElmFunctionCallExpr -> inferFunctionCall(operand)
                is ElmAtomTag -> inferAtom(operand)
                else -> error(operand, "unexpected operand type $operand")
            }

    private fun inferFunctionCall(expr: ElmFunctionCallExpr): Ty {
        if (expr.hasErrors) return TyUnknown()

        val targetTy = inferAtom(expr.target)
        val arguments = expr.arguments.toList()

        // always infer the arguments so that they're added to expressionTypes
        val argTys = arguments.map { inferAtom(it) }

        if (targetTy is TyVar) {
            val ty = TyFunction(argTys, TyVar("a"))
            return when {
                requireAssignable(expr.target, targetTy, ty) -> ty.ret
                else -> TyUnknown()
            }
        }

        fun argCountError(expected: Int): TyUnknown {
            diagnostics += ArgumentCountError(expr, arguments.size, expected)
            return TyUnknown()
        }

        if (!isInferable(targetTy)) return TyUnknown()
        if (targetTy !is TyFunction) return argCountError(0)
        if (arguments.size > targetTy.parameters.size) return argCountError(targetTy.parameters.size)

        var ok = true
        for (i in 0..arguments.lastIndex) {
            // don't short-circuit: we need to check all args
            ok = requireAssignable(arguments[i], argTys[i], targetTy.parameters[i]) && ok
        }

        val resultTy = if (ok) {
            val appliedTy = targetTy.partiallyApply(arguments.size)
            TypeReplacement.replace(appliedTy, replacements)
        } else {
            TyUnknown()
        }
        expressionTypes[expr] = resultTy
        return resultTy
    }

    private fun inferAtom(atom: ElmAtomTag): Ty {
        // For most atoms, we don't try to infer them if they contain errors.
        val ty = when {
            atom is ElmLetInExpr -> inferChild { beginLetInInference(atom) }.ty
            atom is ElmCaseOfExpr -> inferCase(atom)
            atom is ElmParenthesizedExpr -> inferExpression(atom.expression)
            atom.hasErrors -> TyUnknown()
            else -> when (atom) {
                is ElmAnonymousFunctionExpr -> inferLambda(atom)
                is ElmCharConstantExpr -> TyChar
                is ElmFieldAccessExpr -> inferFieldAccess(atom)
                is ElmFieldAccessorFunctionExpr -> inferFieldAccessorFunction(atom)
                is ElmGlslCodeExpr -> TyShader
                is ElmIfElseExpr -> inferIfElse(atom)
                is ElmListExpr -> inferList(atom)
                is ElmNegateExpr -> inferNegateExpression(atom)
                is ElmTupleExpr -> TyTuple(atom.expressionList.map { inferExpression(it) })
                is ElmNumberConstantExpr -> if (atom.isFloat) TyFloat else TyVar("number")
                is ElmOperatorAsFunctionExpr -> inferOperatorAsFunction(atom)
                is ElmRecordExpr -> inferRecord(atom)
                is ElmStringConstantExpr -> TyString
                is ElmTupleConstructorExpr -> TyUnknown()// TODO [drop 0.18] remove this case
                is ElmUnitExpr -> TyUnit()
                is ElmValueExpr -> inferReferenceElement(atom)
                else -> error(atom, "unexpected atom type $atom")
            }
        }
        expressionTypes[atom] = ty
        return ty
    }

    private fun inferOperatorAndPrecedence(operator: ElmOperator): Pair<Ty, OperatorPrecedence?> {
        val ref = operator.reference.resolve() as? ElmInfixDeclaration ?: return TyUnknown() to null
        val precedence = ref.precedence.text.toIntOrNull() ?: return TyUnknown() to null
        val decl = ref.valueExpr?.reference?.resolve() ?: return TyUnknown() to null
        val ty = inferReferencedValueDeclaration(decl.parentOfType())
        return ty to OperatorPrecedence(precedence, ref.associativity)
    }

    private fun inferFieldAccess(expr: ElmFieldAccessExpr): Ty {
        val target = expr.targetExpr
        val targetType = inferFieldAccessTarget(target)
        val targetTy = replacements[targetType]
        val fieldIdentifier = expr.lowerCaseIdentifier ?: return TyUnknown()
        val fieldName = fieldIdentifier.text

        if (targetTy is TyVar) {
            val ty = TyVar("b")
            trackReplacement(targetTy, MutableTyRecord(mutableMapOf(fieldName to ty), TyVar("a")))
            expressionTypes[expr] = ty
            return ty
        }

        if (targetTy is MutableTyRecord) {
            val ty = targetTy.fields.getOrPut(fieldName) { TyVar(nthVarName(targetTy.fields.size)) }
            expressionTypes[expr] = ty
            return ty
        }

        if (targetTy !is TyRecord) {
            if (isInferable(targetTy)) {
                diagnostics += FieldAccessOnNonRecordError(target, targetTy)
            }
            return TyUnknown()
        }

        if (fieldName !in targetTy.fields) {
            if (!targetTy.isSubset) {
                diagnostics += RecordFieldError(fieldIdentifier, fieldName)
            }
            return TyUnknown()
        }

        val ty = targetTy.fields.getValue(fieldName)
        expressionTypes[expr] = ty
        return ty
    }

    private fun inferFieldAccessTarget(target: ElmFieldAccessTargetTag): Ty {
        val ty = when (target) {
            is ElmValueExpr -> inferReferenceElement(target)
            is ElmParenthesizedExpr -> inferExpression(target.expression)
            is ElmRecordExpr -> inferRecord(target)
            is ElmFieldAccessExpr -> inferFieldAccess(target)
            else -> error(target, "unexpected field access target expression")
        }

        expressionTypes[target] = ty
        return ty
    }

    private fun inferLambda(lambda: ElmAnonymousFunctionExpr): Ty {
        // Self-recursion is allowed inside lambdas, so don't copy the active scopes when inferring them
        return inferChild(recursionAllowed = true) { beginLambdaInference(lambda) }.ty
    }

    private fun inferCase(caseOf: ElmCaseOfExpr): Ty {
        val caseOfExprTy = inferExpression(caseOf.expression)
        var ty: Ty? = null
        var errorEncountered = false

        // TODO: check patterns cover possibilities
        for (branch in caseOf.branches) {
            if (branch.hasErrors) break

            // The elm compiler stops issuing diagnostics for branches when it encounters most errors,
            // but will still issue errors within expressions if an earlier type error was encountered
            val pat = branch.pattern
            val branchExpression = branch.expression ?: break

            val childTy = if (errorEncountered) TyUnknown() else caseOfExprTy
            val result = inferChild { beginCaseBranchInference(pat, childTy, branchExpression) }

            if (result.diagnostics.isNotEmpty() || errorEncountered) {
                errorEncountered = true
                continue
            }

            if (ty == null) {
                ty = result.ty
            } else if (!requireAssignable(branchExpression, result.ty, ty)) {
                errorEncountered = true
            }
        }
        return ty ?: TyUnknown()
    }


    private fun inferRecord(record: ElmRecordExpr): Ty {
        val fields = record.fieldList.associate { f ->
            f.lowerCaseIdentifier to inferExpression(f.expression)
        }

        // If there's no base id, then the record is just the type of the fields
        val recordIdentifier = record.baseRecordIdentifier
                ?: return TyRecord(fields.mapKeys { (k, _) -> k.text })

        // If there is a base id, we need to combine it with the fields

        val baseTy = inferReferenceElement(recordIdentifier)

        if (!isInferable(baseTy)) return TyUnknown()

        if (baseTy is TyVar) {
            val extRecord = TyRecord(fields.mapKeys { (k, _) -> k.text }, TyVar(baseTy.name))
            return if (requireAssignable(recordIdentifier, baseTy, extRecord)) {
                extRecord.copy(baseTy = baseTy)
            } else {
                TyUnknown()
            }
        }

        val baseFields = when (baseTy) {
            is TyRecord -> baseTy.fields
            is MutableTyRecord -> baseTy.fields
            else -> {
                diagnostics += RecordBaseIdError(recordIdentifier, baseTy)
                return TyUnknown()
            }
        }

        for ((name, ty) in fields) {
            val expected = baseFields[name.text]
            if (expected == null) {
                when (baseTy) {
                    is TyRecord -> {
                        if (!baseTy.isSubset) diagnostics += RecordFieldError(name, name.text)
                    }
                    is MutableTyRecord -> {
                        baseTy.fields[name.text] = ty
                    }
                }
            } else {
                requireAssignable(name, ty, expected)
            }
        }

        return baseTy
    }

    private fun inferList(expr: ElmListExpr): Ty {
        val expressionList = expr.expressionList
        val expressionTys = expressionList.map { inferExpression(it) }

        for (i in 1..expressionList.lastIndex) {
            // Only issue an error on the first mismatched expression
            if (!requireAssignable(expressionList[i], expressionTys[i], expressionTys[0])) {
                break
            }
        }

        return TyList(expressionTys.firstOrNull() ?: TyVar("a"))
    }

    private fun inferIfElse(ifElse: ElmIfElseExpr): Ty {
        val expressionList = ifElse.expressionList
        if (expressionList.size < 3 || expressionList.size % 2 == 0) return TyUnknown() // incomplete program
        val expressionTys = expressionList.map { inferExpression(it) }

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

    private fun inferReferenceElement(expr: ElmReferenceElement): Ty {
        val ref = expr.reference.resolve() ?: return TyUnknown()

        // If the value is a parameter, its type has already been added to bindings
        getBinding(ref)?.let {
            return when (it) {
                is TyInProgressBinding -> {
                    diagnostics += CyclicDefinitionError(expr)
                    TyUnknown()
                }
                else -> it
            }
        }

        return when (ref) {
            is ElmUnionVariant -> ref.typeExpressionInference().value
            is ElmTypeAliasDeclaration -> {
                val ty = ref.typeExpressionInference().value
                // Record aliases in expressions are constructor functions
                if (ty is TyRecord && ty.fields.isNotEmpty()) {
                    TyFunction(ty.fields.values.toList(), ty)
                } else {
                    ty
                }
            }
            is ElmFunctionDeclarationLeft -> inferReferencedValueDeclaration(ref.parentOfType())
            is ElmPortAnnotation -> ref.typeExpressionInference().value
            is ElmLowerPattern -> {
                // TODO [drop 0.18] remove this check
                if (elementIsInTopLevelPattern(ref)) return TyUnknown()

                // Pattern declarations might not have been inferred yet
                val parentPatternDecl = parentPatternDecl(ref)
                if (parentPatternDecl != null) {
                    inferReferencedValueDeclaration(parentPatternDecl)
                    // Now that the pattern's declaration has been inferred, the pattern has been
                    // added to bindings
                    return getBinding(ref)
                            ?: error(expr, "failed to destructure pattern")
                }

                // All patterns should now be bound
                error(expr, "failed to bind pattern")
            }
            else -> error(ref, "Unexpected reference type")
        }
    }

    private fun inferFieldAccessorFunction(function: ElmFieldAccessorFunctionExpr): Ty {
        val field = function.identifier.text
        val tyVar = TyVar("b")
        return TyFunction(listOf(MutableTyRecord(mutableMapOf(field to tyVar), baseTy = TyVar("a"))), tyVar)
    }

    private fun inferNegateExpression(expr: ElmNegateExpr): Ty {
        val subExpr = expr.expression ?: return TyUnknown()
        val subTy = inferExpression(subExpr)
        return when {
            requireAssignable(subExpr, subTy, TyVar("number")) -> subTy
            else -> TyUnknown()
        }
    }

    private fun inferOperatorAsFunction(op: ElmOperatorAsFunctionExpr): Ty {
        var ref = op.reference.resolve()
        // For operators, we need to resolve the infix declaration to the actual function
        if (ref is ElmInfixDeclaration) {
            ref = ref.valueExpr?.reference?.resolve()
        }
        return inferReferencedValueDeclaration(ref?.parentOfType())
    }

    private fun inferReferencedValueDeclaration(decl: ElmValueDeclaration?): Ty {
        if (decl == null || checkRecursion(decl)) return TyUnknown()
        val existing = resolvedDeclarations[decl]
        if (existing != null) return TypeReplacement.freshenVars(existing)
        // Use the type annotation if there is one
        var ty = decl.typeAnnotation?.typeExpressionInference(rigid = false)?.ty
        // If there's no annotation, do full inference on the function.
        if (ty == null) {
            // First we have to find the parent of the declaration so that it has access to the
            // correct binding/declaration visibility. If there's no parent, we do a cached top-level inference.
            val declParentScope = ancestors.firstOrNull { decl in it.childDeclarations }
            ty = when (declParentScope) {
                null -> decl.inference(activeScopes).ty
                else -> declParentScope.inferChildDeclaration(decl).ty
            }
        }
        resolvedDeclarations[decl] = ty
        return ty
    }

    //</editor-fold>
    //<editor-fold desc="binding">

    /*
     * These functions take an element in a pattern and the Ty that it's binding to, and store the
     * bound names in `bindings`. We can then look up the bound Tys when we infer expressions later
     * in the scope. Note that every name defined in a pattern _must_ be bound to a Ty, even if just
     * to TyUnknown. This is still true in partial programs and other error states. Otherwise,
     * lookups will fail later in the inference.
     */

    /** Cache the type for a pattern binding, and report an error if the name is shadowing something */
    fun setBinding(element: ElmNamedElement, ty: Ty) {
        val elementName = element.name
        if (elementName != null && !shadowableNames.add(elementName) && !elementAllowsShadowing(element)) {
            diagnostics += RedefinitionError(element)
        }

        // Bind the element even if it's shadowing something so that later inference knows it's a parameter
        bindings[element] = ty
        expressionTypes[element] = ty
    }

    /**
     * Bind all names created in a value declaration, either in function parameters or in a pattern
     * declaration.
     *
     * @return a pair of the entire declared type (or [TyUnknown] if no annotation exists), and the
     *   number of parameters in the declaration
     */
    private fun bindParameters(valueDeclaration: ElmValueDeclaration): ParameterBindingResult {
        return when {
            valueDeclaration.functionDeclarationLeft != null -> {
                bindFunctionDeclarationParameters(valueDeclaration, valueDeclaration.functionDeclarationLeft!!)
            }
            valueDeclaration.pattern != null -> {
                bindPatternDeclarationParameters(valueDeclaration, valueDeclaration.pattern!!)
                ParameterBindingResult.Other(0)
            }
            valueDeclaration.operatorDeclarationLeft != null -> {
                // TODO [drop 0.18] remove this case
                // this is 0.18 only, so we aren't going to bother implementing it
                valueDeclaration.declaredNames().associateTo(bindings) { it to TyUnknown() }
                ParameterBindingResult.Other(2)
            }
            else -> ParameterBindingResult.Other(0)
        }
    }


    private fun bindFunctionDeclarationParameters(
            valueDeclaration: ElmValueDeclaration,
            decl: ElmFunctionDeclarationLeft
    ): ParameterBindingResult {
        val typeRefTy = valueDeclaration.typeAnnotation
                ?.typeExpressionInference(rigid = true)?.ty
        val patterns = decl.patterns.toList()

        if (typeRefTy == null) {
            val params = uniqueVars(patterns.size)
            patterns.zip(params) { pat, param -> bindPattern(pat, param, true) }
            return ParameterBindingResult.Unannotated(params, params.size)
        }
        val maxParams = (typeRefTy as? TyFunction)?.parameters?.size ?: 0
        if (patterns.size > maxParams) {
            diagnostics += ParameterCountError(patterns.first(), patterns.last(), patterns.size, maxParams)
            patterns.forEach { pat -> bindPattern(pat, TyUnknown(), true) }
            return ParameterBindingResult.Other(maxParams)
        }

        if (typeRefTy is TyFunction) {
            patterns.zip(typeRefTy.parameters) { pat, ty -> bindPattern(pat, ty, true) }
        }
        return ParameterBindingResult.Annotated(typeRefTy, patterns.size)
    }

    private fun bindPatternDeclarationParameters(valueDeclaration: ElmValueDeclaration, pattern: ElmPattern) {
        // For case branches and pattern declarations like `(x,y) = (1,2)`, we need to finish
        // inferring the expression before we can bind the parameters. In these cases, it's an error
        // to use a name from the pattern in its expression (e.g. `{x} = {x=x}`). We need to know
        // about the declared names during inference in order to detect this, but we don't know the
        // type until inference is complete. Since we can't bind the names to a type, we instead
        // bind all the names to sentinel values, then infer the expression and check for cyclic
        // references, then finally overwrite the sentinels with the proper inferred type.
        val declaredNames = pattern.descendantsOfType<ElmLowerPattern>()
        declaredNames.associateTo(bindings) { it to TyInProgressBinding }
        val bodyTy = inferExpression(valueDeclaration.expression)
        // Now we can overwrite the sentinels we set earlier with the inferred type
        bindPattern(pattern, bodyTy, false)
        // If we made a mistake during binding, the sentinels might not have been
        // overwritten, so do a sanity check here.
        for (name in declaredNames) {
            if (getBinding(name) == TyInProgressBinding) {
                error(name, "failed to bind parameter")
            }
        }
    }

    private fun bindPattern(pat: ElmFunctionParamOrPatternChildTag, type: Ty, isParameter: Boolean) {
        val ty = replacements[type]
        when (pat) {
            is ElmAnythingPattern -> {
            }
            is ElmConsPattern -> {
                if (isParameter) {
                    diagnostics += PartialPatternError(pat)
                    bindConsPattern(pat, TyUnknown())
                } else {
                    bindConsPattern(pat, ty)
                }
            }
            is ElmListPattern -> {
                if (isParameter) {
                    diagnostics += PartialPatternError(pat)
                    bindListPattern(pat, TyUnknown())
                } else {
                    bindListPattern(pat, ty)
                }
            }
            is ElmConstantTag -> {
                if (isParameter) diagnostics += PartialPatternError(pat)
            }
            is ElmPattern -> {
                bindPattern(pat.child, ty, isParameter)
                pat.patternAs?.let { bindPattern(it, ty, isParameter) }
            }
            is ElmLowerPattern -> setBinding(pat, ty)
            is ElmRecordPattern -> bindRecordPattern(pat, ty, isParameter)
            is ElmTuplePattern -> bindTuplePattern(pat, ty, isParameter)
            is ElmUnionPattern -> bindUnionPattern(pat, ty, isParameter)
            is ElmUnitExpr -> requireAssignable(pat, ty, TyUnit(), patternBinding = true)
            else -> error(pat, "unexpected pattern type")
        }
    }

    private fun bindConsPattern(pat: ElmConsPattern, ty: Ty) {
        bindListPatternParts(pat, pat.parts.toList(), ty, true)
    }

    private fun bindListPattern(pat: ElmListPattern, ty: Ty) {
        bindListPatternParts(pat, pat.parts.toList(), ty, false)
    }

    private fun bindListPatternParts(pat: ElmPatternChildTag, parts: List<ElmPatternChildTag>, type: Ty, isCons: Boolean) {
        val ty = bindIfVar(pat, type) { TyList(TyVar("a")) }

        if (!isInferable(ty) || ty !is TyUnion || !ty.isTyList) {
            if (isInferable(ty)) {
                diagnostics += TypeMismatchError(pat, TyList(TyVar("a")), ty, patternBinding = true)
            }
            parts.forEach { bindPattern(it, TyUnknown(), false) }
            return
        }

        val innerTy = ty.parameters[0] // lists only have one parameter

        for (part in parts.dropLast(1)) {
            val t = if (part is ElmListPattern) ty else innerTy
            bindPattern(part, t, false)
        }

        if (parts.isNotEmpty()) {
            // The last part of a cons pattern binds to the entire list type, while in list patterns
            // it binds to the element type.
            bindPattern(parts.last(), if (isCons) ty else innerTy, false)
        }
    }

    private fun bindUnionPattern(pat: ElmUnionPattern, type: Ty, isParameter: Boolean) {
        val variant = pat.reference.resolve() as? ElmUnionVariant
        val variantTy = variant?.typeExpressionInference()?.value

        if (variantTy == null || !isInferable(variantTy)) {
            pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
            return
        }

        fun issueError(actual: Int, expected: Int) {
            diagnostics += ArgumentCountError(pat, actual, expected, true)
            pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
        }

        val argumentPatterns = pat.argumentPatterns.toList()

        if (variantTy is TyFunction) {
            val ty = bindIfVar(pat, type) { variantTy.ret }
            if (requireAssignable(pat, ty, variantTy.ret)) {
                if (argumentPatterns.size != variantTy.parameters.size) {
                    issueError(argumentPatterns.size, variantTy.parameters.size)
                } else {
                    for ((p, t) in argumentPatterns.zip(variantTy.parameters)) {
                        // The other option is an UpperCaseQID, which doesn't bind anything
                        if (p is ElmFunctionParamOrPatternChildTag) bindPattern(p, t, isParameter)
                    }
                }
            } else {
                pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
            }
        } else {
            val ty = bindIfVar(pat, type) { variantTy }
            if (requireAssignable(pat, ty, variantTy) && argumentPatterns.isNotEmpty()) {
                issueError(argumentPatterns.size, 0)
            } else {
                pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
            }
        }
    }

    private fun bindTuplePattern(pat: ElmTuplePattern, type: Ty, isParameter: Boolean) {
        val patternList = pat.patternList
        val ty = bindIfVar(pat, type) { TyTuple(uniqueVars(patternList.size)) }

        if (ty !is TyTuple || ty.types.size != patternList.size) {
            patternList.forEach { bindPattern(it, TyUnknown(), isParameter) }
            if (isInferable(ty)) {
                val actualTy = TyTuple(uniqueVars(patternList.size))
                diagnostics += TypeMismatchError(pat, actualTy, ty, patternBinding = true)
            }
            return
        }

        patternList.zip(ty.types) { p, t ->
            bindPattern(p, t, isParameter)
        }
    }

    private fun bindRecordPattern(pat: ElmRecordPattern, type: Ty, isParameter: Boolean) {
        val fields = pat.lowerPatternList

        val ty = bindIfVar(pat, type) {
            TyRecord(
                    fields = fields.zip(uniqueVars(fields.size)) { f, t -> f.name to t }.toMap(),
                    baseTy = TyVar("a")
            )
        }

        if (ty !is TyRecord || fields.any { it.name !in ty.fields }) {
            if (isInferable(ty)) {
                val actualTyParams = fields.zip(uniqueVars(fields.size)) { f, v -> f.name to v }
                val actualTy = TyRecord(actualTyParams.toMap())

                // For pattern declarations, the elm compiler issues diagnostics on the expression
                // rather than the pattern, but it's easier for us to issue them on the pattern instead.
                diagnostics += TypeMismatchError(pat, actualTy, ty, patternBinding = true)
            }

            for (f in fields) {
                bindPattern(f, TyUnknown(), isParameter)
            }

            return
        }

        for (f in fields) {
            bindPattern(f, ty.fields.getValue(f.name), isParameter)
        }
    }

    //</editor-fold>
    //<editor-fold desc="unification">

    /*
     * These functions test that a Ty can be assigned to another Ty. The tests are lenient, so no
     * diagnostic will be reported if either type is TyUnknown.
     */

    private fun requireAssignable(
            element: PsiElement,
            ty1: Ty,
            ty2: Ty,
            endElement: ElmPsiElement? = null,
            patternBinding: Boolean = false
    ): Boolean {
        val assignable = try {
            assignable(ty1, ty2)
        } catch (e: InfiniteTypeException) {
            diagnostics += InfiniteTypeError(element)
            return false
        }
        if (!assignable) {
            // To match the elm compiler, we report type errors on case expression as if they're
            // errors on the first branch expression
            val start = if (endElement == null && element is ElmCaseOfExpr) {
                element.branches.firstOrNull()?.expression ?: element
            } else {
                element
            }
            val t1 = TypeReplacement.replace(ty1, replacements)
            val t2 = TypeReplacement.replace(ty2, replacements)
            diagnostics += TypeMismatchError(start, t1, t2, endElement, patternBinding)
        }
        return assignable
    }

    /** Return `false` if [type1] definitely cannot be assigned to [type2] */
    private fun assignable(type1: Ty, type2: Ty): Boolean {
        val ty1 = replacements[type1]
        val ty2 = replacements[type2]

        val result = ty1 === ty2 || ty1 is TyUnknown || ty2 is TyUnknown || if (ty1 !is TyVar && ty2 is TyVar) {
            nonVarAssignableToVar(ty1, ty2)
        } else when (ty1) {
            is TyVar -> if (ty2 is TyVar) varsAssignable(ty1, ty2) else nonVarAssignableToVar(ty2, ty1)
            is TyTuple -> ty2 is TyTuple
                    && ty1.types.size == ty2.types.size
                    && allAssignable(ty1.types, ty2.types)
            is TyRecord -> {
                ty2 is TyRecord && recordAssignable(ty1, ty2) ||
                        ty2 is MutableTyRecord && mutableRecordAssignable(ty2, ty1)
            }
            is MutableTyRecord -> {
                ty2 is TyRecord && mutableRecordAssignable(ty1, ty2) ||
                        ty2 is MutableTyRecord && mutableRecordAssignable(ty1, ty2.asRecord())
            }
            is TyUnion -> ty2 is TyUnion
                    && ty1.name == ty2.name
                    && ty1.module == ty2.module
                    && allAssignable(ty1.parameters, ty2.parameters)
            is TyFunction -> ty2 is TyFunction && funcsAssignable(ty1, ty2)
            is TyUnit -> ty2 is TyUnit
            is TyUnknown -> true
            TyInProgressBinding -> error("should never try to assign $ty1")
        }

        if (result) trackReplacement(ty1, ty2)
        return result
    }

    private fun mutableRecordAssignable(ty1: MutableTyRecord, ty2: TyRecord): Boolean {
        if (!recordAssignable(ty1.asRecord(), ty2)) return false
        ty1.fields += ty2.fields
        return true
    }

    private fun recordAssignable(ty1: TyRecord, ty2: TyRecord): Boolean {
        fun fieldsAssignable(t1: TyRecord, t2: TyRecord, strict: Boolean): Boolean {
            return t1.fields.all { (k, v) ->
                t2.fields[k]?.let { assignable(v, it) } ?: !strict
            }
        }

        // Subset record tys are created from extension record declarations or field accessor functions

        val result = when {
            // e.g. passing an extension record argument to an extension record parameter
            ty1.isSubset && ty2.isSubset -> {
                // whatever fields they have in common have to have the same types
                fieldsAssignable(ty1, ty2, strict = false)
            }
            // e.g. invoking a field accessor function with a concrete record
            // e.g. passing a concrete record into an extension record parameter
            !ty1.isSubset && ty2.isSubset -> {
                fieldsAssignable(ty2, ty1, strict = true)
            }
            // e.g. passing a field accessor into a parameter requiring a function taking a concrete record
            // e.g. passing an extension record to a concrete parameter
            ty1.isSubset && !ty2.isSubset -> {
                fieldsAssignable(ty1, ty2, strict = true)
            }
            // e.g. returning a concrete record from a function declaring it returns a concrete record
            !ty1.isSubset && !ty2.isSubset -> {
                ty1.fields.size == ty2.fields.size && fieldsAssignable(ty1, ty2, strict = true)
            }
            else -> error("impossible")
        }

        // If we're assigning a concrete record to an extension, set the type of the extension base
        // var to the concrete record.
        if (result && ty1.baseTy == null && ty2.baseTy is TyVar) {
            trackReplacement(ty1, ty2.baseTy)
        }
        return result
    }

    private fun allAssignable(ty1: List<Ty>, ty2: List<Ty>): Boolean {
        // don't short circuit so that all types get applied
        return ty1.size == ty2.size && ty1.zip(ty2) { l, r -> assignable(l, r) }.all { it }
    }

    private fun funcsAssignable(ty1: TyFunction, ty2: TyFunction): Boolean {
        // We need to handle currying. If ty1 has fewer parameters than ty2, it's an error. If ty1
        // has more parameters than ty2, all extra parameters are curried into a function that is
        // assigned to the last parameter of ty2.
        val tys1 = ty1.allTys
        val tys2 = ty2.allTys

        fun makeFunc(tys: List<Ty>) = when {
            tys.size == 1 -> tys.single()
            else -> TyFunction(tys.dropLast(1), tys.last())
        }

        val sharedSize = minOf(tys1.size, tys2.size) - 1
        val sharedAssignable = allAssignable(tys1.take(sharedSize), tys2.take(sharedSize))
        val tailAssignable = assignable(makeFunc(tys1.drop(sharedSize)), makeFunc(tys2.drop(sharedSize)))

        return sharedAssignable && tailAssignable
    }

    /**
     * Check if a [ty2] can that compares unequal to a [ty1] can be unified with it
     */
    private fun varsAssignable(ty1: TyVar, ty2: TyVar): Boolean {
        val tc1 = getTypeclassName(ty1)
        val tc2 = getTypeclassName(ty2)

        return when {
            !ty1.rigid && tc1 == null -> true
            !ty1.rigid && tc1 != null -> {
                typeclassesCompatable(tc1, tc2, unconstrainedAllowed = !ty2.rigid) ||
                        !ty2.rigid && typeclassesConstrainToCompappend(tc1, tc2)
            }
            ty1.rigid && tc1 == null -> !ty2.rigid && tc2 == null
            ty1.rigid && tc1 != null && ty2.rigid -> tc1 == tc2
            ty1.rigid && tc1 != null && !ty2.rigid -> typeclassesCompatable(tc1, tc2, unconstrainedAllowed = !ty2.rigid)
            else -> error("impossible")
        }
    }

    private fun nonVarAssignableToVar(ty: Ty, tyVar: TyVar): Boolean {
        // Vars with certain names are treated as typeclasses that only unify with a limited set of
        // types.
        //
        //  From the elm guide:
        //
        //  - `number` permits `Int` and `Float`
        //  - `appendable` permits `String` and `List a`
        //  - `comparable` permits `Int`, `Float`, `Char`, `String`, and lists/tuples of `comparable` values
        //  - `compappend` permits `String` and `List comparable`
        //
        //  Not listed in the elm guide:
        //
        //  - `number`     permits `comparable`
        //  - `comparable` permits `compappend`, `number` and lists/tuples of `number`
        //  - `appendable` permits `compappend`
        //  - `compappend` permits `List compappend`

        fun List<Ty>.allAssignableTo(typeclass: String): Boolean = all { assignable(it, TyVar(typeclass)) }

        return when {
            tyVar.name.startsWith("number") -> when (ty) {
                is TyUnion -> ty.isTyFloat || ty.isTyInt
                else -> false
            }
            tyVar.name.startsWith("appendable") -> when (ty) {
                is TyUnion -> ty.isTyString || ty.isTyList
                else -> false
            }
            tyVar.name.startsWith("comparable") -> when (ty) {
                is TyTuple -> ty.types.allAssignableTo("comparable")
                is TyUnion -> ty.isTyFloat
                        || ty.isTyInt
                        || ty.isTyChar
                        || ty.isTyString
                        || ty.isTyList && ty.parameters.run {
                    allAssignableTo("comparable") || allAssignableTo("number")
                }
                else -> false
            }
            tyVar.name.startsWith("compappend") -> when (ty) {
                is TyUnion -> ty.isTyString || ty.isTyList && ty.parameters.run {
                    allAssignableTo("comparable") || allAssignableTo("compappend")
                }
                else -> false
            }
            else -> !tyVar.rigid
        }
    }

    private fun typeclassesCompatable(name1: String, name2: String?, unconstrainedAllowed: Boolean = true): Boolean {
        return when {
            name2 == null -> unconstrainedAllowed
            name1 == name2 -> true
            name1 == "number" && name2 == "comparable" -> true
            name1 == "comparable" && name2 == "number" -> true
            name1 == "comparable" && (name2 == "number" || name2 == "compappend") -> true
            name1 == "compappend" && (name2 == "comparable" || name2 == "appendable") -> true
            else -> false
        }
    }

    private fun typeclassesConstrainToCompappend(tc1: String?, tc2: String?): Boolean {
        return when (tc1) {
            "comparable" -> tc2 == "appendable" || tc2 == "compappend"
            "appendable" -> tc2 == "comparable" || tc2 == "compappend"
            else -> false
        }
    }

    private fun trackReplacement(ty1: Ty, ty2: Ty) {
        if (ty1 === ty2) return

        fun assign(k: TyVar, v: Ty) {
            if (containsVar(v, k)) throw InfiniteTypeException()
            replacements[k] = v
        }

        // assigning anything to a variable constrains the type of that variable
        if (ty2 is TyVar && (ty2 !in replacements || ty1 !is TyVar && replacements[ty2] is TyVar)) {
            if (ty1 is TyVar) {
                val tc1 = getTypeclassName(ty1)
                val tc2 = getTypeclassName(ty2)
                if (tc1 == null && tc2 != null) {
                    // There's an edge case where an assignment like `a => number`
                    // should constrain `a` to be a `number`, rather than `number` to be an `a`.
                    assign(ty1, ty2)
                } else if (!ty1.rigid && !ty2.rigid && typeclassesConstrainToCompappend(tc1, tc2)) {
                    // There's another edge case where unifying flex `comparable` with flex
                    // `appendable` creates a new constraint with typeclass `compappend`.
                    // Assigning flex `comparable` or `appendable` to flex `compappend` will also
                    // constrain the arguments.
                    assign(ty1, if (tc2 == "compappend") ty2 else TyVar("compappend"))
                } else if (!ty1.rigid && !ty2.rigid && tc1 == "comparable" && tc2 == "number") {
                    // `comparable` can be constrained to `number`
                    assign(ty1, ty2)
                } else {
                    // Normally, you have assignments like `Int => number` which constrains `number` to
                    // be an `Int`.
                    assign(ty2, ty1)
                }
            } else {
                // Normally, you have assignments like `Int => number` which constrains `number` to
                // be an `Int`.
                assign(ty2, ty1)
            }
        }
        // unification: assigning a var to a type also constrains the var, but only if not
        // assigning it to another var, since we just handled that case above.
        if (ty1 is TyVar && ty2 !is TyVar && ty1 !in replacements) {
            // If the var is being assigned to an extension record, make the constraint mutable,
            // since other field constraints could be added later.
            if (ty2 is TyRecord && ty2.isSubset) {
                assign(ty1, MutableTyRecord(ty2.fields.toMutableMap(), ty2.baseTy))
            } else {
                assign(ty1, ty2)
            }
        }
    }

    /**
     * If the [ty] corresponding to [elem] is a [TyVar], bind [ty] to [default]; otherwise return [ty].
     *
     * This is used to constrain unannotated parameters during binding. Note that this binds via
     * [requireAssignable], so if [ty] is rigid, this will take care of generating a diagnostic
     * rather than constraining the variable.
     */
    private inline fun bindIfVar(elem: ElmPsiElement, ty: Ty, default: () -> Ty): Ty {
        return when (ty) {
            is TyVar -> default().also { requireAssignable(elem, ty, it, patternBinding = true) }
            else -> ty
        }
    }

    //</editor-fold>
}

/**
 * @property expressionTypes the types for any psi elements inferred that should be available to inspections
 * @property diagnostics any errors encountered during inference
 * @property ty the return type of the function or expression being inferred
 */
data class InferenceResult(val expressionTypes: Map<ElmPsiElement, Ty>,
                           val diagnostics: List<ElmDiagnostic>,
                           val ty: Ty) {
    fun elementType(element: ElmPsiElement): Ty = expressionTypes[element] ?: TyUnknown()
}

data class ParameterizedInferenceResult<T>(
        val diagnostics: List<ElmDiagnostic>,
        val value: T
)

val ElmPsiElement.moduleName: String
    get() = elmFile.getModuleDecl()?.name ?: ""


/** Return [count] [TyVar]s named a, b, ... z, a1, b1, ... */
private fun uniqueVars(count: Int): List<TyVar> {
    return varNames().take(count).map { TyVar(it) }.toList()
}

/** Return the nearest [ElmValueDeclaration] if it declares a pattern, or `null` otherwise */
private fun parentPatternDecl(element: ElmPsiElement): ElmValueDeclaration? {
    val decl = element.parentOfType<ElmValueDeclaration>()
    return if (decl?.pattern == null) null else decl
}

// TODO [drop 0.18] remove this refDecl check
private fun elementIsInTopLevelPattern(element: ElmPsiElement): Boolean {
    // top-level patterns are unsupported after 0.18
    return parentPatternDecl(element)?.parent is ElmFile
}

/** A [ty] and the [start] and [end] elements of the expression that created it */
private data class TyAndRange(val start: ElmPsiElement, val end: ElmPsiElement, val ty: Ty) {
    constructor(element: ElmPsiElement, ty: Ty) : this(element, element, ty)
}

private fun elementAllowsShadowing(element: ElmPsiElement): Boolean {
    return elementIsInTopLevelPattern(element) || (element.elmProject?.isElm18 ?: false)
}

fun isInferable(ty: Ty): Boolean = ty !is TyUnknown

/** extracts the typeclass from a [TyVar] name if it is a typeclass */
private val TYPECLASS_REGEX = Regex("(number|appendable|comparable|compappend)\\d*")

/** Extract the typeclass for a var name if it is one, or null if it's a normal var*/
fun getTypeclassName(ty: TyVar): String? = TYPECLASS_REGEX.matchEntire(ty.name)?.groups?.get(1)?.value

/** Throw an [IllegalStateException] with [message] augmented with information about [element] */
fun error(element: ElmPsiElement, message: String): Nothing {
    val file = element.containingFile
    val location = if (file == null) "" else {
        val fileText = file.text
        val textRange = element.textRange
        val startOffset = textRange.startOffset
        val lineNum = fileText.asSequence().take(startOffset).count { it == '\n' } + 1
        val lineStart = fileText.lastIndexOf('\n', startIndex = startOffset)
        val start = startOffset - lineStart
        val end = textRange.endOffset - lineStart
        " [${file.name}:$lineNum:$start-$end]"
    }

    val text = element.text.let { if (it.length > 25) "${it.take(25)}…" else it }.replace("\n", "↵")
    error("$message$location <${element.elementType}: '$text'>")
}


private sealed class ParameterBindingResult {
    abstract val count: Int

    data class Annotated(val ty: Ty, override val count: Int) : ParameterBindingResult()
    data class Unannotated(val params: List<Ty>, override val count: Int) : ParameterBindingResult()
    data class Other(override val count: Int) : ParameterBindingResult()
}

/** dangerous shallow copy of a mutable record for performance, use `toRecord` if the result isn't discarded. */
private fun MutableTyRecord.asRecord(): TyRecord = TyRecord(fields, baseTy)

/** Return true if [tyVar] is referenced anywhere withing [ty] */
private fun containsVar(ty: Ty, tyVar: TyVar): Boolean = when (ty) {
    is TyVar -> ty == tyVar
    is TyTuple -> ty.types.any { containsVar(it, tyVar) }
    is TyRecord -> ty.fields.values.any { containsVar(it, tyVar) } || (ty.baseTy != null && containsVar(ty.baseTy, tyVar))
    is MutableTyRecord -> containsVar(ty.asRecord(), tyVar)
    is TyFunction -> containsVar(ty.ret, tyVar) || ty.parameters.any { containsVar(it, tyVar) }
    is TyUnion, is TyUnit, is TyUnknown, TyInProgressBinding -> false
}

private class InfiniteTypeException : Exception()
