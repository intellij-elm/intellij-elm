package org.elm.ide.inspections

import com.intellij.psi.PsiDocumentManager
import com.intellij.util.DocumentUtil
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmAnythingPattern
import org.elm.lang.core.psi.elements.ElmCaseOfExpr
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*
import org.elm.utils.getIndent

/**
 * This class can detect missing branches for case expressions and insert them into the PSI in place.
 */
class MissingCaseBranchAdder(val element: ElmCaseOfExpr) {
    sealed class Result {
        /** There are missing union variant branches */
        data class MissingVariants(val variants: VariantParameters) : Result() {
            init {
                require(variants.isNotEmpty()) { "MissingVariants may not be empty" }
            }
        }

        /** There are missing branches, but we can't offer suggestions */
        object NoSuggestions : Result()

        /** There are no missing branches that we can detect */
        object NoMissing : Result()
    }

    private val document = element.containingFile
            ?.let { PsiDocumentManager.getInstance(element.project).getDocument(it) }

    // This should be a lazy {}, but using it causes a compilation error due to conflicting
    // declarations.
    val result: Result
        get() {

            if (_result == null) {
                _result = if (document == null) Result.NoMissing else calcMissingBranches()
            }
            return _result!!
        }
    private var _result: Result? = null

    fun addMissingBranches() {
        val result = this.result as? Result.MissingVariants ?: return

        val patterns = result.variants.map { (name, params) ->
            (listOf(name) + params.map { it.renderParam() }).joinToString(" ")
        }

        addPatterns(patterns)
    }

    fun addWildcardBranch() {
        if (result is Result.NoMissing) return
        addPatterns(listOf("_"))
    }

    private fun addPatterns(patterns: List<String>) {
        val factory = ElmPsiFactory(element.project)
        val existingBranches = element.branches
        val indent = document!!.getIndent(element.startOffset) + "    "
        val elements = factory.createCaseOfBranches(indent, patterns)
        // Add the two or first generated branch, which are the indent
        // and newline
        var start = elements.first().prevSibling.prevSibling

        // Add the virtual open token if there isn't one already
        if (existingBranches.isNotEmpty()) {
            start = start.prevSibling
        }

        element.addRange(start, elements.last())

        // Without a value in the last branch, the virtual end token ends up before the trailing
        // whitespace, so we can't grab it from the factory element and need to add it separately.
        val trailingWs = factory.createElements("\n$indent    ")
        element.addRange(trailingWs.first(), trailingWs.last())
    }

    private fun calcMissingBranches(): Result {
        val defaultResult = if (element.branches.isEmpty()) Result.NoSuggestions else Result.NoMissing

        val inference = element.findInference() ?: return defaultResult

        // This only works on case expressions with a union type for now
        val exprTy = element.expression?.let { inference.elementType(it) } as? TyUnion
                ?: return defaultResult

        val declaration = findTypeDeclaration(exprTy) ?: return defaultResult

        val allBranches = declaration.variantInference().value
        val missingBranches = allBranches.toMutableMap()

        for (branch in element.branches) {
            val pat = branch.pattern.child
            when (pat) {
                is ElmAnythingPattern -> return Result.NoMissing // covers all cases
                is ElmUnionPattern -> {
                    missingBranches.remove(pat.referenceName)
                }
                else -> return Result.NoMissing // invalid pattern
            }
        }

        if (missingBranches.isEmpty()) {
            return Result.NoMissing
        }

        val qualifierPrefix = ModuleScope.getQualifierForTypeName(element.elmFile, exprTy.module, exprTy.name)
                ?: ""

        return Result.MissingVariants(missingBranches.mapKeys { (k, _) -> qualifierPrefix + k })
    }

    private fun findTypeDeclaration(exprTy: TyUnion): ElmTypeDeclaration? {
        val candidates = ElmLookup.findByNameAndModule<ElmTypeDeclaration>(exprTy.name, exprTy.module, element.elmFile)
        return when {
            candidates.size < 2 -> candidates.firstOrNull()
            else -> {
                // Multiple modules have the same name and define a type of the same name.
                // Since the Elm compiler forbids you from importing a module whose name
                // is ambiguous, the only way for this to be valid is if they are actually
                // the *same* module.
                candidates.firstOrNull { it.elmFile == element.elmFile }
            }
        }
    }
}
