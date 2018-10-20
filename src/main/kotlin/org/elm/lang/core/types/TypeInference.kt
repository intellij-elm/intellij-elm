package org.elm.lang.core.types

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.diagnostics.*
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.OperatorAssociativity.NON
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import java.util.*

private val TYPE_INFERENCE_KEY: Key<CachedValue<InferenceResult>> = Key.create("TYPE_INFERENCE_KEY")

fun ElmValueDeclaration.inference(): InferenceResult = inference(emptySet())

private fun ElmValueDeclaration.inference(activeScopes: Set<ElmValueDeclaration>): InferenceResult {
    return CachedValuesManager.getCachedValue(this, TYPE_INFERENCE_KEY) {
        // Elm lets you shadow imported names, including auto-imported names, so only count names
        // declared in this file as shadowable.
        val shadowableNames = ModuleScope(elmFile).getVisibleValues().topLevel.mapNotNullTo(mutableSetOf()) { it.name }
        val result = InferenceScope(shadowableNames, activeScopes.toMutableSet(), null).beginDeclarationInference(this)
        Result.create(result, project.modificationTracker, modificationTracker)
    }
}

/**
 * @property shadowableNames names of declared elements that will cause a shadowing error if redeclared
 * @property activeScopes scopes that are currently being inferred, to detect invalid recursion; copied from parent
 */
private class InferenceScope(
        private val shadowableNames: MutableSet<String>,
        private val activeScopes: MutableSet<ElmValueDeclaration>,
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

    private val ancestors: Sequence<InferenceScope> get() = generateSequence(this) { it.parent }

    private fun getBinding(e: ElmNamedElement): Ty? = ancestors.mapNotNull { it.bindings[e] }.firstOrNull()

    //<editor-fold desc="entry points">
    /*
     * These functions begin inference for elements that contain lexical scopes. Only one
     * `begin` function should be called on a scope instance.
     */

    fun beginDeclarationInference(declaration: ElmValueDeclaration): InferenceResult {
        // If the declaration has any syntax errors, we don't run inference on it. Trying to resolve
        // references in expressions that contain syntax errors can result in infinite recursion and
        // surprising bugs.
        if (checkBadRecursion(declaration) || PsiTreeUtil.hasErrorElements(declaration)) {
            return InferenceResult(bindings, diagnostics, TyUnknown)
        }

        activeScopes += declaration

        val (declaredTy, paramCount) = bindParameters(declaration)

        val expr = declaration.expression
        var bodyTy: Ty = TyUnknown
        if (expr != null) {
            bodyTy = inferExpression(expr)

            // If the body is just a let expression, show the diagnostic on its expression rather than the whole body.
            val parts = expr.parts.toList()
            val errorExpr = (parts.singleOrNull() as? ElmLetIn)?.expression ?: expr

            val expected = (declaredTy as? TyFunction)?.partiallyApply(paramCount) ?: declaredTy
            requireAssignable(errorExpr, bodyTy, expected)
        }

        val ty = if (declaredTy === TyUnknown) bodyTy else declaredTy
        return InferenceResult(bindings, diagnostics, ty)
    }

    private fun beginLambdaInference(lambda: ElmAnonymousFunction): InferenceResult {
        // TODO [unification] infer param types
        val patternList = lambda.patternList
        val paramVars = uniqueVars(patternList.size)
        patternList.zip(paramVars).forEach { (p, t) -> bindPattern(p, t, true) }
        val bodyTy = inferExpression(lambda.expression)
        val ty = TyFunction(paramVars, bodyTy)
        return InferenceResult(bindings, diagnostics, ty)
    }

    private fun beginLetInInference(letIn: ElmLetIn): InferenceResult {
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
        return InferenceResult(emptyMap(), diagnostics, exprTy)
    }

    private fun beginCaseBranchInference(pattern: ElmPattern, caseTy: Ty, branchExpression: ElmExpression): InferenceResult {
        bindPattern(pattern, caseTy, false)
        val ty = inferExpression(branchExpression)
        return InferenceResult(bindings, diagnostics, ty)
    }

    private inline fun inferChild(activeScopes: MutableSet<ElmValueDeclaration> = this.activeScopes.toMutableSet(),
                                  block: InferenceScope.() -> InferenceResult): InferenceResult {
        val result = InferenceScope(shadowableNames.toMutableSet(), activeScopes, this).block()
        diagnostics += result.diagnostics
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
                    setBinding(p, result.bindingType(p))
                    shadowableNames += p.name
                }
            }
        }
        return result
    }

    private fun checkBadRecursion(declaration: ElmValueDeclaration): Boolean {
        val isRecursive = declaration in activeScopes
        if (isRecursive) {
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

    private fun inferExpression(expr: ElmExpression?): Ty {
        if (expr == null || elementContainsErrors(expr)) return TyUnknown

        val parts: List<ElmExpressionPartTag> = expr.parts.toList()

        // Get the operator types and precedences. We don't have to worry about invalid
        // code like `1 + + 1`, since it won't parse as an expression.
        val operatorPrecedences = HashMap<ElmOperator, OperatorPrecedence>(parts.size / 2)
        val operatorTys = HashMap<ElmOperator, TyFunction>(parts.size / 2)
        var lastPrecedence: OperatorPrecedence? = null
        for (part in parts) {
            if (part is ElmOperator) {
                val (ty, precedence) = inferOperatorAndPrecedence(part)
                when {
                    precedence == null || ty !is TyFunction || ty.parameters.size != 2 -> return TyUnknown
                    precedence.associativity == NON && lastPrecedence?.associativity == NON -> {
                        // Non-associative operators can't be chained directly with other non-associative
                        // operators.
                        diagnostics += NonAssociativeOperatorError(expr, part)
                        return TyUnknown
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
        fun validateTree(tree: BinaryExprTree<ElmExpressionPartTag>): RangeTy {
            return when (tree) {
                is BinaryExprTree.Operand -> {
                    RangeTy(tree.operand, inferOperand(tree.operand as ElmOperandTag))
                }
                is BinaryExprTree.Binary -> {
                    val l = validateTree(tree.left)
                    val r = validateTree(tree.right)
                    val func = operatorTys[tree.operator]!!
                    requireAssignable(l.start, l.ty, func.parameters[0], l.end)
                    requireAssignable(r.start, r.ty, func.parameters[1], r.end)
                    RangeTy(l.start, r.end, func.ret)
                }
            }
        }

        val result = validateTree(BinaryExprTree.parse(parts, operatorPrecedences))
        return result.ty
    }

    private fun inferOperand(operand: ElmOperandTag): Ty {
        return when (operand) {
            is ElmAnonymousFunction -> inferLambda(operand)
            is ElmCaseOf -> inferCase(operand)
            is ElmCharConstant -> TyChar
            is ElmExpression -> inferExpression(operand) // parenthesized expression
            is ElmExpressionWithAccessor -> inferExpressionWithAccessor(operand)
            is ElmFieldAccess -> inferFieldAccess(operand)
            is ElmFieldAccessorFunction -> inferFieldAccessorFunction(operand)
            is ElmFunctionCall -> inferFunctionCall(operand)
            is ElmGlslCode -> TyShader
            is ElmIfElse -> inferIfElse(operand)
            is ElmLetIn -> inferChild { beginLetInInference(operand) }.ty
            is ElmList -> inferList(operand)
            is ElmNegateExpression -> inferNegateExpression(operand)
            is ElmNonEmptyTuple -> TyTuple(operand.expressionList.map { inferExpression(it) })
            is ElmNumberConstant -> if (operand.isFloat) TyFloat else TyVar("number") // TODO[unification] handle `number1`,`number2`,...
            is ElmOperatorAsFunction -> inferOperatorAsFunction(operand)
            is ElmRecord -> inferRecord(operand)
            is ElmRecordWithAccessor -> inferRecordWithAccessor(operand)
            is ElmStringConstant -> TyString
            is ElmTupleConstructor -> TyUnknown // TODO [drop 0.18] remove this case
            is ElmUnit -> TyUnit
            is ElmValueExpr -> inferReferenceElement(operand)
            else -> error("unexpected operand type $operand")
        }
    }

    private fun inferOperatorAndPrecedence(operator: ElmOperator): Pair<Ty, OperatorPrecedence?> {
        val ref = operator.reference.resolve() as? ElmInfixDeclaration ?: return TyUnknown to null
        val precedence = ref.precedence.text.toIntOrNull() ?: return TyUnknown to null
        val decl = ref.valueExpr?.reference?.resolve() ?: return TyUnknown to null
        val ty = inferChildValueDeclaration(decl.parentOfType())
        return ty to OperatorPrecedence(precedence, ref.associativity)
    }

    private fun inferFieldAccess(fieldAccess: ElmFieldAccess): Ty {
        val baseElement = fieldAccess.referenceNameElement
        val baseTy = inferReferenceElement(fieldAccess)
        val fields = fieldAccess.lowerCaseIdentifierList.drop(1)
        return inferAccessorChain(baseElement, baseTy, fields)
    }

    private fun inferExpressionWithAccessor(expressionWithAccessor: ElmExpressionWithAccessor): Ty {
        val baseElement = expressionWithAccessor.expression
        val baseTy = inferExpression(baseElement)
        val fields = expressionWithAccessor.accessor.lowerCaseIdentifierList
        return inferAccessorChain(baseElement, baseTy, fields)
    }

    private fun inferRecordWithAccessor(recordWithAccessor: ElmRecordWithAccessor): Ty {
        val baseElement = recordWithAccessor.record
        val baseTy = inferRecord(baseElement)
        val fields = recordWithAccessor.accessor.lowerCaseIdentifierList
        return inferAccessorChain(baseElement, baseTy, fields)
    }

    private fun inferAccessorChain(baseElement: PsiElement, baseTy: Ty, fields: List<PsiElement>): Ty {
        var element = baseElement
        var ty: Ty = baseTy

        for (field in fields) {
            if (ty === TyUnknown || ty is TyVar) { // TODO[unification] infer vars
                return TyUnknown
            }

            if (ty !is TyRecord) {
                diagnostics += TypeMismatchError(element, ty, TyVar("record"))
                return TyUnknown
            }

            val text = field.text
            if (text !in ty.fields) {
                diagnostics += RecordFieldError(field, text)
                return TyUnknown
            }

            ty = ty.fields[text]!!
            element = field
        }

        return ty
    }

    private fun inferLambda(lambda: ElmAnonymousFunction): Ty {
        // Self-recursion is allowed inside lambdas, so don't copy the active scopes when inferring them
        return inferChild(activeScopes = mutableSetOf()) { beginLambdaInference(lambda) }.ty
    }

    private fun inferCase(caseOf: ElmCaseOf): Ty {
        // Currently, if the type of a case expression doesn't match the value it's assigned to, we issue a
        // diagnostic on the entire case expression. The elm compiler only issues the diagnostic on
        // the first branch expression.

        val caseOfExprTy = inferExpression(caseOf.expression)
        var ty: Ty = TyUnknown
        var errorEncountered = false

        // TODO: check patterns cover possibilities
        for (branch in caseOf.branches) {
            // The elm compiler stops issuing diagnostics for branches when it encounters most errors,
            // but will still issue errors within expressions if an earlier type error was encountered
            val pat = branch.pattern ?: break
            val branchExpression = branch.expression ?: break

            if (errorEncountered) {
                // just check for internal errors
                bindPattern(pat, TyUnknown, false)
                inferExpression(branchExpression)
                continue
            }

            val result = inferChild { beginCaseBranchInference(pat, caseOfExprTy, branchExpression) }

            if (result.diagnostics.isNotEmpty()) {
                errorEncountered = true
                continue
            }

            if (assignable(result.ty, ty)) {
                ty = result.ty
            } else {
                diagnostics += TypeMismatchError(branchExpression, result.ty, ty)
                errorEncountered = true
            }
        }
        return ty
    }


    private fun inferRecord(record: ElmRecord): Ty {
        val recordIdentifier = record.baseRecordIdentifier
        if (recordIdentifier == null) {
            val fields = record.fieldList.associate { f ->
                f.lowerCaseIdentifier.text to inferExpression(f.expression)
            }
            return TyRecord(fields)
        }

        val baseTy = inferReferenceElement(recordIdentifier)
        if (baseTy !is TyRecord) {
            diagnostics += RecordBaseIdError(recordIdentifier, baseTy)
            return TyUnknown
        }

        val fields = record.fieldList.associate { f ->
            f.lowerCaseIdentifier to inferExpression(f.expression)
        }
        for ((name, ty) in fields) {
            val expected = baseTy.fields[name.text]
            if (expected == null) {
                diagnostics += RecordFieldError(name, name.text)
            } else {
                requireAssignable(name, ty, expected)
            }
        }

        return baseTy
    }

    private fun inferList(expr: ElmList): Ty {
        val expressionList = expr.expressionList
        val expressionTys = expressionList.map { inferExpression(it) }

        for (i in 1..expressionList.lastIndex) {
            // Only issue an error on the first mismatched expression
            if (!requireAssignable(expressionList[i], expressionTys[i], expressionTys[0])) {
                break
            }
        }

        return TyList(expressionTys.firstOrNull() ?: TyUnknown)
    }

    private fun inferIfElse(ifElse: ElmIfElse): Ty {
        val expressionList = ifElse.expressionList
        if (expressionList.size < 3 || expressionList.size % 2 == 0) return TyUnknown // incomplete program
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
        val ref = expr.reference.resolve() ?: return TyUnknown

        // If the value is a parameter, its type has already been added to bindings
        getBinding(ref)?.let {
            return when (it) {
                is TyInProgressBinding -> {
                    diagnostics += CyclicDefinitionError(expr)
                    TyUnknown
                }
                else -> it
            }
        }

        return when (ref) {
            is ElmUnionMember -> unionMemberType(ref)
            is ElmTypeAliasDeclaration -> typeAliasDeclarationType(ref)
            is ElmFunctionDeclarationLeft -> inferChildValueDeclaration(ref.parentOfType())
            is ElmPortAnnotation -> portAnnotationType(ref)
            is ElmLowerPattern -> {
                // TODO [drop 0.18] remove this check
                if (elementIsInTopLevelPattern(ref)) return TyUnknown

                // Pattern declarations might not have been inferred yet
                val parentPatternDecl = parentPatternDecl(ref)
                if (parentPatternDecl != null) {
                    return inferChildValueDeclaration(parentPatternDecl)
                }

                // All patterns should now be bound
                error("failed to bind pattern for expr of type ${expr.elementType}: '${expr.text}'")
            }
            else -> error("Unexpected reference type ${ref.elementType}")
        }
    }

    private fun inferFieldAccessorFunction(function: ElmFieldAccessorFunction): Ty {
        val field = function.identifier.text
        val tyVar = TyVar("b")
        return TyFunction(listOf(TyRecord(mapOf(field to tyVar), baseName = "a")), tyVar)
    }

    private fun inferNegateExpression(expr: ElmNegateExpression): Ty {
        val subExpr = expr.expression
        val subTy = inferExpression(subExpr)
        // TODO [unification] restrict vars to only number
        return when (subTy) {
            TyInt, TyFloat, TyUnknown, is TyVar -> subTy
            else -> {
                diagnostics += TypeMismatchError(subExpr!!, subTy, TyVar("number"))
                TyUnknown
            }
        }
    }

    private fun inferFunctionCall(call: ElmFunctionCall): Ty {
        val inferredTy = inferOperand(call.target)

        val arguments = call.arguments.toList()

        fun argCountError(expected: Int): TyUnknown {
            diagnostics += ArgumentCountError(call, arguments.size, expected)
            return TyUnknown
        }

        val targetTy = when (inferredTy) {
            is TyFunction -> inferredTy
            is TyRecord -> when {
                // Record constructor
                inferredTy.alias != null -> TyFunction(inferredTy.fields.values.toList(), inferredTy)
                else -> return argCountError(0)
            }
            TyUnknown, is TyVar -> return TyUnknown // TODO[unification] infer vars
            else -> return argCountError(0)
        }

        if (arguments.size > targetTy.parameters.size) {
            return argCountError(targetTy.parameters.size)
        }

        for ((arg, paramTy) in arguments.zip(targetTy.parameters)) {
            val argTy = inferOperand(arg)
            requireAssignable(arg, argTy, paramTy)
        }

        return targetTy.partiallyApply(arguments.size)
    }

    private fun inferOperatorAsFunction(op: ElmOperatorAsFunction): Ty {
        var ref = op.reference.resolve()
        // For operators, we need to resolve the infix declaration to the actual function
        if (ref is ElmInfixDeclaration) {
            ref = ref.valueExpr?.reference?.resolve()
        }
        return inferChildValueDeclaration(ref?.parentOfType())
    }

    private fun inferChildValueDeclaration(decl: ElmValueDeclaration?): Ty {
        if (decl == null || checkBadRecursion(decl)) return TyUnknown
        val existing = resolvedDeclarations[decl]
        if (existing != null) return existing
        // Use the type annotation if there is one
        var ty = decl.typeAnnotation?.typeRef?.let { typeRefType(it) }
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
    /** Cache the type for a pattern binding, or report an error if the name is shadowing something */
    fun setBinding(element: ElmNamedElement, ty: Ty) {
        val elementName = element.name
        if (elementName != null && !shadowableNames.add(elementName) && !elementIsInTopLevelPattern(element)) {
            diagnostics += RedefinitionError(element)
        }

        // Bind the element even if it's shadowing something so that later inference knows it's a parameter
        bindings[element] = ty
    }

    /** @return a pair of the entire declared type (or [TyUnknown] if no annotation exists), and the
     *   number of parameters in the declaration
     */
    private fun bindParameters(valueDeclaration: ElmValueDeclaration): Pair<Ty, Int> {
        return when {
            valueDeclaration.functionDeclarationLeft != null -> {
                bindFunctionDeclarationParameters(valueDeclaration, valueDeclaration.functionDeclarationLeft!!)
            }
            valueDeclaration.pattern != null -> {
                bindPatternDeclarationParameters(valueDeclaration, valueDeclaration.pattern!!)
            }
            valueDeclaration.operatorDeclarationLeft != null -> {
                // TODO [drop 0.18] remove this case
                // this is 0.18 only, so we aren't going to bother implementing it
                valueDeclaration.declaredNames().associateTo(bindings) { it to TyUnknown }
                TyUnknown to 2
            }
            else -> TyUnknown to 0
        }
    }

    private fun bindFunctionDeclarationParameters(
            valueDeclaration: ElmValueDeclaration,
            decl: ElmFunctionDeclarationLeft
    ): Pair<Ty, Int> {
        val typeRef = valueDeclaration.typeAnnotation?.typeRef
        val patterns = decl.patterns.toList()

        if (typeRef == null) {
            patterns.forEach { pat -> bindPattern(pat, TyUnknown, true) }
            return when {
                patterns.isEmpty() -> TyUnknown to 0
                else -> TyFunction(patterns.map { TyUnknown }, TyUnknown) to patterns.size
            }
        }

        val typeRefTy = typeRefType(typeRef)
        if (typeRefTy is TyFunction) {
            patterns.zip(typeRefTy.parameters).forEach { (pat, ty) -> bindPattern(pat, ty, true) }
        }
        return typeRefTy to patterns.size
    }

    private fun bindPatternDeclarationParameters(
            valueDeclaration: ElmValueDeclaration,
            pattern: ElmPattern
    ): Pair<Ty, Int> {
        // For case branches and pattern declarations like `(x,y) = (1,2)`, we need to finish
        // inferring the expression before we can bind the parameters. In these cases, it's an error
        // to use a name from the pattern in its expression (e.g. `{x} = {x=x}`). Since we don't
        // have a Ty to bind to yet, we first bind all the names to sentinel values, then infer the
        // expression and check for cyclic references, then finally overwrite the sentinels with the inferred
        // type.
        val declaredNames = pattern.descendantsOfType<ElmLowerPattern>()
        declaredNames.associateTo(bindings) { it to TyInProgressBinding }
        val bodyTy = inferExpression(valueDeclaration.expression)
        // Now we can overwrite the sentinels we set earlier with the inferred type
        bindPattern(pattern, bodyTy, false)
        // If we made a mistake during binding, the sentinels might not have been
        // overwritten, so do a sanity check here.
        for (name in declaredNames) {
            if (getBinding(name) == TyInProgressBinding) {
                error("failed to bind element of type ${name.elementType} with name '${name.text}'")
            }
        }
        return bodyTy to 1
    }

    private fun bindPattern(pat: ElmFunctionParamOrPatternChildTag, ty: Ty, isParameter: Boolean) {
        when (pat) {
            is ElmAnythingPattern -> {
            }
            is ElmConsPattern -> {
                if (isParameter) {
                    diagnostics += PartialPatternError(pat)
                    bindConsPattern(pat, TyUnknown)
                } else {
                    bindConsPattern(pat, ty)
                }
            }
            is ElmListPattern -> {
                if (isParameter) {
                    diagnostics += PartialPatternError(pat)
                    bindListPattern(pat, TyUnknown)
                } else {
                    bindListPattern(pat, ty)
                }
            }
            is ElmConstantTag -> {
                if (isParameter) diagnostics += PartialPatternError(pat)
            }
            is ElmPattern -> {
                bindPattern(pat.child, ty, isParameter)
                bindPatternAs(pat.patternAs, ty)
            }
            is ElmLowerPattern -> setBinding(pat, ty)
            is ElmRecordPattern -> bindRecordPattern(pat, ty, isParameter)
            is ElmTuplePattern -> bindTuplePattern(pat, ty, isParameter)
            is ElmUnionPattern -> bindUnionPattern(pat, isParameter)
            is ElmUnit -> {
            }
            else -> error("unexpected type $pat")
        }
    }

    private fun bindConsPattern(pat: ElmConsPattern, ty: Ty) {
        bindListPatternParts(pat, pat.parts.toList(), ty)
    }

    private fun bindListPattern(pat: ElmListPattern, ty: Ty) {
        bindListPatternParts(pat, pat.parts.toList(), ty)
    }

    private fun bindListPatternParts(pat: ElmPatternChildTag, parts: List<ElmPatternChildTag>, ty: Ty) {
        // Cons and list patterns both have the same semantics.
        // `[a, b]` is equivalent to `a :: b :: []`, i.e. a list of length exactly two.
        // The last part is bound to the tail of the list, and any leading parts are bound to
        // individual elements.
        if (ty !is TyUnknown && (ty !is TyUnion || !ty.isTyList)) {
            diagnostics += TypeMismatchError(pat, TyList(TyVar("a")), ty)
            parts.forEach { bindPattern(it, TyUnknown, false) }
            return
        }

        val elementTy = (ty as? TyUnion)?.parameters?.get(0) ?: TyUnknown

        for (part in parts.dropLast(1)) {
            val t = if (part is ElmListPattern) ty else elementTy
            bindPattern(part, t, false)
        }

        if (parts.isNotEmpty()) {
            bindPattern(parts.last(), ty, false)
        }
    }


    private fun bindPatternAs(pat: ElmPatternAs?, ty: Ty) {
        if (pat != null) setBinding(pat, ty)
    }

    private fun bindUnionPattern(pat: ElmUnionPattern, isParameter: Boolean) {
        // If the referenced union member isn't a constructor (e.g. `Nothing`), then there's nothing to bind
        val memberTy = (pat.reference.resolve() as? ElmUnionMember)?.let { unionMemberType(it) } ?: return
        val argumentPatterns = pat.argumentPatterns.toList()

        fun issueError(actual: Int, expected: Int) {
            diagnostics += ArgumentCountError(pat, actual, expected)
            pat.namedParameters.forEach { setBinding(it, TyUnknown) }
        }

        if (memberTy is TyFunction) {
            if (argumentPatterns.size != memberTy.parameters.size) {
                issueError(argumentPatterns.size, memberTy.parameters.size)
            } else {
                for ((p, t) in argumentPatterns.zip(memberTy.parameters)) {
                    // The other option is an UpperCaseQID, which doesn't bind anything
                    if (p is ElmFunctionParamOrPatternChildTag) bindPattern(p, t, isParameter)
                }
            }
        } else if (argumentPatterns.isNotEmpty()) {
            issueError(argumentPatterns.size, 0)
        }
    }

    private fun bindTuplePattern(pat: ElmTuplePattern, ty: Ty, isParameter: Boolean) {
        if (ty !is TyTuple) {
            pat.patternList.forEach { bindPattern(it, TyUnknown, isParameter) }
            // TODO [unification] handle binding vars
            if (ty !is TyVar && ty !is TyUnknown) {
                val actualTy = TyTuple(uniqueVars(pat.patternList.size))
                diagnostics += TypeMismatchError(pat, actualTy, ty)
            }
            return
        }
        pat.patternList
                .zip(ty.types)
                .forEach { (pat, type) -> bindPattern(pat.child, type, isParameter) }
    }

    private fun bindRecordPattern(pat: ElmRecordPattern, ty: Ty, isParameter: Boolean) {
        val lowerPatternList = pat.lowerPatternList
        if (ty !is TyRecord || lowerPatternList.any { it.name !in ty.fields }) {
            // TODO[unification] bind to vars
            if (ty !is TyVar && ty !is TyUnknown) {
                val actualTyParams = lowerPatternList.map { it.name }.zip(uniqueVars(lowerPatternList.size))
                val actualTy = TyRecord(actualTyParams.toMap())

                // For pattern declarations, the elm compiler issues diagnostics on the expression
                // rather than the pattern, but it's easier for us to issue them on the pattern instead.
                diagnostics += TypeMismatchError(pat, actualTy, ty)
            }

            for (p in lowerPatternList) {
                bindPattern(p, TyUnknown, isParameter)
            }

            return
        }

        for (id in lowerPatternList) {
            val fieldTy = ty.fields[id.name]!!
            bindPattern(id, fieldTy, isParameter)
        }
    }

    //</editor-fold>
    //<editor-fold desc="coercion">
    /*
     * These functions test that a Ty can be assigned to another Ty. The tests are lenient, so no
     * diagnostic will be reported if either type is TyUnkown. Other than `requireAssignable`, none
     * of the functions access the scope.
     */
    private fun requireAssignable(element: PsiElement, ty1: Ty, ty2: Ty, endElement: ElmPsiElement? = null): Boolean {
        val assignable = assignable(ty1, ty2)
        if (!assignable) {
            diagnostics += TypeMismatchError(element, ty1, ty2, endElement)
        }
        return assignable
    }

    /** Return `false` if [ty1] definitely cannot be assigned to [ty2] */
    private fun assignable(ty1: Ty, ty2: Ty): Boolean {
        // TODO[unification] assignability for vars
        return ty1 === ty2 || ty2 is TyVar || ty2 is TyUnknown || when (ty1) {
            is TyVar -> true
            is TyTuple -> ty2 is TyTuple
                    && ty1.types.size == ty2.types.size
                    && allAssignable(ty1.types, ty2.types)
            is TyRecord -> ty2 is TyRecord && recordAssignable(ty1, ty2)
                    || ty2 is TyFunction && recordAssignableToFunction(ty1, ty2)
            is TyUnion -> ty2 is TyUnion
                    && ty1.name == ty2.name
                    && ty1.module == ty2.module
                    && allAssignable(ty1.parameters, ty2.parameters)
            is TyFunction -> ty2 is TyFunction
                    && allAssignable(ty1.allTys, ty2.allTys)
            // object tys are covered by the identity check above
            TyShader, TyUnit -> false
            TyUnknown -> true
            TyInProgressBinding -> error("should never try to assign TyInProgressBinding")
        }
    }

    private fun recordAssignable(ty1: TyRecord, ty2: TyRecord): Boolean {
        fun fieldsAssignable(t1: TyRecord, t2: TyRecord): Boolean {
            return t1.fields.all { (k, v) -> t2.fields[k]?.let { assignable(v, it) } ?: false }
        }
        if (ty2.isSubset) {
            return fieldsAssignable(ty2, ty1)
        }
        val correctSize = when {
            ty1.isSubset -> ty1.fields.size <= ty2.fields.size
            else -> ty1.fields.size == ty2.fields.size
        }
        return correctSize && fieldsAssignable(ty1, ty2)
    }

    private fun recordAssignableToFunction(record: TyRecord, function: TyFunction): Boolean {
        return record.alias != null
                && assignable(record, function.ret)
                && allAssignable(record.fields.values.toList(), function.parameters)
    }

    private fun allAssignable(ty1: List<Ty>, ty2: List<Ty>) =
            ty1.size == ty2.size &&
                    ty1.zip(ty2).all { (l, r) -> assignable(l, r) }

    //</editor-fold>
}


/**
 * @property ty the return type of the function or expression being inferred
 */
data class InferenceResult(val bindings: Map<ElmNamedElement, Ty>,
                           val diagnostics: List<ElmDiagnostic>,
                           val ty: Ty) {
    fun bindingType(element: ElmNamedElement): Ty = bindings[element] ?: TyUnknown
}

val ElmPsiElement.moduleName: String
    get() = elmFile.getModuleDecl()?.name ?: ""


/** Return [count] [TyVar]s named a, b, ... z, a1, b1, ... */
private fun uniqueVars(count: Int): List<TyVar> {
    val s = "abcdefghijklmnopqrstuvwxyz"
    return (0 until count).map {
        TyVar(s[it % s.length] + if (it >= s.length) (it / s.length).toString() else "")
    }
}

private fun parentPatternDecl(element: ElmPsiElement): ElmValueDeclaration? {
    val decl = element.parentOfType<ElmValueDeclaration>()
    return if (decl?.pattern == null) null else decl
}

// TODO [drop 0.18] remove this refDecl check
private fun elementIsInTopLevelPattern(element: ElmPsiElement): Boolean {
    // top-level patterns are unsupported after 0.18
    return parentPatternDecl(element)?.parent is ElmFile
}

private fun elementContainsErrors(element: ElmPsiElement): Boolean {
    return element.childOfType<PsiErrorElement>() != null
}

private data class RangeTy(val start: ElmPsiElement, val end: ElmPsiElement, val ty: Ty) {
    constructor(element: ElmPsiElement, ty: Ty) : this(element, element, ty)
}
