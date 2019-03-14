package org.elm.lang.core.types

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.*
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
    private val replacements: MutableMap<TyVar, Ty> = parent?.replacements ?: mutableMapOf()

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
        // references in expressions that contain syntax errors can result in infinite recursion and
        // surprising bugs.
        // We don't currently infer recursive functions in order to avoid infinite loops in the inference.
        if (checkRecursion(declaration) || assignee == null || PsiTreeUtil.hasErrorElements(assignee)) {
            return InferenceResult(expressionTypes, diagnostics, TyUnknown())
        }

        activeScopes += declaration

        // We need to bind parameters before we infer the body since the body can reference the
        // params in functions
        val binding = bindParameters(declaration)

        // The body of pattern declarations is inferred as part of parameter binding, so there's no
        // more to do here.
        if (declaration.pattern != null) {
            return InferenceResult(expressionTypes, diagnostics, TyUnknown())
        }

        // For function declarations, we need to infer the body and check that it matches the
        // annotation, if there is one.
        val expr = declaration.expression
        var bodyTy: Ty = TyUnknown()
        if (expr != null && !PsiTreeUtil.hasErrorElements(expr)) {
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
                if (binding.count == 0) TypeReplacement.replace(bodyTy, replacements)
                else TypeReplacement.replace(TyFunction(binding.params, bodyTy).uncurry(), replacements)
            }
            is ParameterBindingResult.Other -> bodyTy
        }
        return InferenceResult(expressionTypes, diagnostics, ty)
    }

    private fun beginLambdaInference(lambda: ElmAnonymousFunctionExpr): InferenceResult {
        val patternList = lambda.patternList
        val paramVars = uniqueVars(patternList.size)
        patternList.zip(paramVars).forEach { (p, t) -> bindPattern(p, t, true) }
        val bodyTy = inferExpression(lambda.expression)
        val ty = TypeReplacement.replace(TyFunction(paramVars, bodyTy).uncurry(), replacements)
        return InferenceResult(expressionTypes, diagnostics, ty)
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

    //</editor-fold>
    //<editor-fold desc="inference">
    /*
     * These functions take elements from expressions and return their Ty. If the Ty can't be
     * inferred due to program error or unimplemented functionality, TyUnknown is returned.
     * These functions recurse down into children elements, if any, and report diagnostics on them.
     */

    private fun inferExpression(expr: ElmExpressionTag?): Ty {
        if (expr == null || PsiTreeUtil.hasErrorElements(expr)) return TyUnknown()
        return when (expr) {
            is ElmBinOpExpr -> inferBinOpExpr(expr)
            is ElmFunctionCallExpr -> inferFunctionCall(expr)
            is ElmAtomTag -> inferAtom(expr)
            else -> error(expr, "unexpected expression type: $expr")
        }
    }

    private fun inferBinOpExpr(expr: ElmBinOpExpr): Ty {
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

        val ok = (0..arguments.lastIndex).all { i ->
            requireAssignable(arguments[i], argTys[i], targetTy.parameters[i])
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
        val ty = when (atom) {
            is ElmAnonymousFunctionExpr -> inferLambda(atom)
            is ElmCaseOfExpr -> inferCase(atom)
            is ElmCharConstantExpr -> TyChar
            is ElmFieldAccessExpr -> inferFieldAccess(atom)
            is ElmFieldAccessorFunctionExpr -> inferFieldAccessorFunction(atom)
            is ElmGlslCodeExpr -> TyShader
            is ElmIfElseExpr -> inferIfElse(atom)
            is ElmLetInExpr -> inferChild { beginLetInInference(atom) }.ty
            is ElmListExpr -> inferList(atom)
            is ElmNegateExpr -> inferNegateExpression(atom)
            is ElmTupleExpr -> TyTuple(atom.expressionList.map { inferExpression(it) })
            is ElmNumberConstantExpr -> if (atom.isFloat) TyFloat else TyVar("number")
            is ElmOperatorAsFunctionExpr -> inferOperatorAsFunction(atom)
            is ElmParenthesizedExpr -> inferExpression(atom.expression)
            is ElmRecordExpr -> inferRecord(atom)
            is ElmStringConstantExpr -> TyString
            is ElmTupleConstructorExpr -> TyUnknown()// TODO [drop 0.18] remove this case
            is ElmUnitExpr -> TyUnit()
            is ElmValueExpr -> inferReferenceElement(atom)
            else -> error(atom, "unexpected atom type $atom")
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

    private fun inferFieldAccess(fieldAccess: ElmFieldAccessExpr): Ty {
        val target = fieldAccess.targetExpr
        val baseTy = inferFieldAccessTarget(target)

        if (baseTy !is TyRecord) {
            if (isInferable(baseTy)) {
                val errorElem = if (target is ElmFieldAccessExpr) target.lowerCaseIdentifier
                        ?: target else target
                diagnostics += TypeMismatchError(errorElem, baseTy, TyVar("record"))
            }
            return TyUnknown()
        }

        val fieldIdentifier = fieldAccess.lowerCaseIdentifier ?: return TyUnknown()
        if (fieldIdentifier.text !in baseTy.fields) {
            // TODO[unification] once we know all available fields, we can be stricter about subset records.
            if (!baseTy.isSubset) {
                diagnostics += RecordFieldError(fieldIdentifier, fieldIdentifier.text)
            }
            return TyUnknown()
        }

        val ty = baseTy.fields.getValue(fieldIdentifier.text)
        expressionTypes[fieldAccess] = ty
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
        // Currently, if the type of a case expression doesn't match the value it's assigned to, we issue a
        // diagnostic on the entire case expression. The elm compiler only issues the diagnostic on
        // the first branch expression.

        val caseOfExprTy = inferExpression(caseOf.expression)
        var ty: Ty? = null
        var errorEncountered = false

        // TODO: check patterns cover possibilities
        for (branch in caseOf.branches) {
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

        // If there's no base id, then we the record is just the type of the fields
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
                diagnostics += RecordBaseIdError(recordIdentifier, baseTy)
                TyUnknown()
            }
        }

        if (baseTy !is TyRecord) {
            diagnostics += RecordBaseIdError(recordIdentifier, baseTy)
            return TyUnknown()
        }

        for ((name, ty) in fields) {
            val expected = baseTy.fields[name.text]
            if (expected == null) {
                if (!baseTy.isSubset) diagnostics += RecordFieldError(name, name.text)
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
        return TyFunction(listOf(TyRecord(mapOf(field to tyVar), baseTy = TyVar("a"))), tyVar)
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
        var ty = decl.typeAnnotation?.typeExpressionInference()?.value
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
        val typeRefTy = valueDeclaration.typeAnnotation?.typeExpressionInference()?.value
        val patterns = decl.patterns.toList()

        if (typeRefTy == null) {
            val params = uniqueVars(patterns.size)
            patterns.zip(params).forEach { (pat, param) -> bindPattern(pat, param, true) }
            return ParameterBindingResult.Unannotated(params, params.size)
        }
        val maxParams = (typeRefTy as? TyFunction)?.parameters?.size ?: 0
        if (patterns.size > maxParams) {
            diagnostics += ParameterCountError(patterns.first(), patterns.last(), patterns.size, maxParams)
            patterns.forEach { pat -> bindPattern(pat, TyUnknown(), true) }
            return ParameterBindingResult.Other(maxParams)
        }

        if (typeRefTy is TyFunction) {
            patterns.zip(typeRefTy.parameters).forEach { (pat, ty) -> bindPattern(pat, ty, true) }
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
        val ty = getReplacement(type)
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
            is ElmUnionPattern -> bindUnionPattern(pat, isParameter)
            is ElmUnitExpr -> requireAssignable(pat, ty, TyUnit())
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
        // vars always get bound to `List a` before further constraints
        val ty = when (type) {
            is TyVar -> TyList(TyVar("a")).also { requireAssignable(pat, type, it) }
            else -> type
        }

        if (!isInferable(ty) || ty !is TyUnion || !ty.isTyList) {
            if (isInferable(ty)) {
                diagnostics += TypeMismatchError(pat, TyList(TyVar("a")), ty)
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

    private fun bindUnionPattern(pat: ElmUnionPattern, isParameter: Boolean) {
        // If the referenced union variant isn't a constructor (e.g. `Nothing`), then there's nothing
        // to bind.
        val variantTy = (pat.reference.resolve() as? ElmUnionVariant)?.typeExpressionInference()?.value
        val argumentPatterns = pat.argumentPatterns.toList()

        fun issueError(actual: Int, expected: Int) {
            diagnostics += ArgumentCountError(pat, actual, expected)
            pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
        }

        if (variantTy is TyFunction) {
            if (argumentPatterns.size != variantTy.parameters.size) {
                issueError(argumentPatterns.size, variantTy.parameters.size)
            } else {
                for ((p, t) in argumentPatterns.zip(variantTy.parameters)) {
                    // The other option is an UpperCaseQID, which doesn't bind anything
                    if (p is ElmFunctionParamOrPatternChildTag) bindPattern(p, t, isParameter)
                }
            }
        } else if (variantTy == null) {
            // null variantTy means the reference didn't resolve
            pat.namedParameters.forEach { setBinding(it, TyUnknown()) }
        } else if (argumentPatterns.isNotEmpty()) {
            issueError(argumentPatterns.size, 0)
        }
    }

    private fun bindTuplePattern(pat: ElmTuplePattern, type: Ty, isParameter: Boolean) {
        val patternList = pat.patternList

        // vars always get bound to `(a, b) or `(a, b, c)` before further constraints
        val ty = when (type) {
            is TyVar -> TyTuple(uniqueVars(patternList.size)).also { requireAssignable(pat, type, it) }
            else -> type
        }

        if (ty !is TyTuple || ty.types.size != patternList.size) {
            patternList.forEach { bindPattern(it, TyUnknown(), isParameter) }
            if (isInferable(ty)) {
                val actualTy = TyTuple(uniqueVars(patternList.size))
                diagnostics += TypeMismatchError(pat, actualTy, ty)
            }
            return
        }

        patternList.zip(ty.types).forEach { (pat, type) ->
            bindPattern(pat, type, isParameter)
        }
    }

    private fun bindRecordPattern(pat: ElmRecordPattern, ty: Ty, isParameter: Boolean) {
        val lowerPatternList = pat.lowerPatternList
        if (ty !is TyRecord || lowerPatternList.any { it.name !in ty.fields }) {
            // TODO[unification] bind to vars
            if (isInferable(ty)) {
                val actualTyParams = lowerPatternList.map { it.name }.zip(uniqueVars(lowerPatternList.size))
                val actualTy = TyRecord(actualTyParams.toMap())

                // For pattern declarations, the elm compiler issues diagnostics on the expression
                // rather than the pattern, but it's easier for us to issue them on the pattern instead.
                diagnostics += TypeMismatchError(pat, actualTy, ty)
            }

            for (p in lowerPatternList) {
                bindPattern(p, TyUnknown(), isParameter)
            }

            return
        }

        for (id in lowerPatternList) {
            val fieldTy = ty.fields.getValue(id.name)
            bindPattern(id, fieldTy, isParameter)
        }
    }

    //</editor-fold>
    //<editor-fold desc="unification">

    /*
     * These functions test that a Ty can be assigned to another Ty. The tests are lenient, so no
     * diagnostic will be reported if either type is TyUnknown. Other than `requireAssignable`, none
     * of these functions access any scope `InferenceScope` properties.
     */

    private fun requireAssignable(
            element: PsiElement,
            ty1: Ty,
            ty2: Ty,
            endElement: ElmPsiElement? = null
    ): Boolean {
        val assignable = assignable(ty1, ty2)
        if (!assignable) {
            val t1 = TypeReplacement.replace(ty1, replacements)
            val t2 = TypeReplacement.replace(ty2, replacements)
            diagnostics += TypeMismatchError(element, t1, t2, endElement)
        }
        return assignable
    }

    /** Return `false` if [ty1] definitely cannot be assigned to [ty2] */
    private fun assignable(type1: Ty, type2: Ty): Boolean {
        val ty1 = getReplacement(type1)
        val ty2 = getReplacement(type2)

        val result = ty1 === ty2 || ty1 is TyUnknown || ty2 is TyUnknown || if (ty2 is TyVar) {
            varAssignable(ty2, ty1)
        } else when (ty1) {
            is TyVar -> varAssignable(ty1, ty2)
            is TyTuple -> ty2 is TyTuple
                    && ty1.types.size == ty2.types.size
                    && allAssignable(ty1.types, ty2.types)
            is TyRecord -> ty2 is TyRecord && recordAssignable(ty1, ty2)
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

        // We need to unify the base var with the record being assigned for the case where we have
        // an alias to an extension record constructor. We only do this if the records are
        // different, since that would incorrectly create a recursive type.
        if (result && ty2.baseTy is TyVar && (ty1.baseTy == null || ty1.fields != ty2.fields)) {
            trackReplacement(ty1, ty2.baseTy)
        }
        return result
    }

    private fun allAssignable(ty1: List<Ty>, ty2: List<Ty>): Boolean {
        // don't short circuit so that all types get applied
        return ty1.size == ty2.size && ty1.zip(ty2).map { (l, r) -> assignable(l, r) }.all { it }
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

    // TODO rigid vars
    /** Check if a [ty] can that compares unequal to a [tyVar] can be unified with it */
    private fun varAssignable(tyVar: TyVar, ty: Ty): Boolean {
        // Vars with certain names are treated as typeclasses that only unify with a limited set of
        // types.
        //
        //  From the elm guide:
        //
        //  - `number` permits `Int` and `Float`
        //  - `appendable` permits `String` and `List a`
        //  - `comparable` permits `Int`, `Float`, `Char`, `String`, and lists/tuples of `comparable` values
        //  - `compappend` permits `String` and `List comparable`

        fun List<Ty>.allComparable(): Boolean = all { assignable(it, TyVar("comparable")) }

        return when {
            tyVar.name.startsWith("number") -> when (ty) {
                is TyUnion -> ty.isTyFloat || ty.isTyInt
                is TyVar -> typeclassCompatable("number", tyVar.name, ty.name)
                else -> false
            }
            tyVar.name.startsWith("appendable") -> when (ty) {
                is TyUnion -> ty.isTyString || ty.isTyList
                is TyVar -> typeclassCompatable("appendable", tyVar.name, ty.name)
                else -> false
            }
            tyVar.name.startsWith("comparable") -> when (ty) {
                is TyTuple -> ty.types.allComparable()
                is TyUnion -> ty.isTyFloat
                        || ty.isTyInt
                        || ty.isTyChar
                        || ty.isTyString
                        || ty.isTyList && ty.parameters.allComparable()
                is TyVar -> ty.name.startsWith("number") || typeclassCompatable("comparable", tyVar.name, ty.name)
                else -> false
            }
            tyVar.name.startsWith("compappend") -> when (ty) {
                is TyUnion -> ty.isTyString || ty.isTyList && ty.parameters.allComparable()
                is TyVar -> ty.name.startsWith("number") || typeclassCompatable("compappend", tyVar.name, ty.name)
                else -> false
            }
            else -> true
        }
    }

    /**
     * Check if a var with [name1] of a var in [typeclass] is compatible with the typeclass of a var with [name2]
     *
     * Specifics of each typeclass are checked outside this function.
     */
    private fun typeclassCompatable(typeclass: String, name1: String, name2: String): Boolean {
        // all unconstrained vars can unify with constrained vars, so if there's no typeclass, we
        // always return true
        val otherTypclassName = getTypeclassName(name2) ?: return true
        // any numbered var can be unify with an unnumbered var in the same typeclass. If they're
        // both numbered, they have to match exactly.
        return otherTypclassName == typeclass &&
                (name1 == name2 || otherTypclassName == typeclass || name1 == typeclass)
    }

    private fun trackReplacement(ty1: Ty, ty2: Ty) {
        if (ty1 == ty2) return
        // assigning anything to a variable fixes the type of that variable
        if (ty2 is TyVar && (ty2 !in replacements || ty1 !is TyVar && replacements[ty2] is TyVar)) {
            replacements[ty2] = ty1
        }
        // unification: assigning a var to a type also restricts the vars type, but only if not
        // assigning it to another var.
        if (ty1 is TyVar && ty2 !is TyVar && ty1 !in replacements) {
            replacements[ty1] = ty2
        }
    }

    private fun getReplacement(ty: Ty): Ty {
        if (ty !is TyVar) return ty

        var node: TyVar = ty
        var parent = replacements[node]

        // treat the replacements as a disjoint-set and use Tarjan's algorithm for path compression
        // to keep access near constant time
        while (parent is TyVar) {
            val grandparent = replacements[parent] ?: return parent
            replacements[node] = grandparent
            node = parent
            parent = grandparent
        }

        return parent ?: node
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
fun getTypeclassName(name: String): String? = TYPECLASS_REGEX.matchEntire(name)?.value

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
