package org.elm.ide.inspections

import com.intellij.psi.search.GlobalSearchScope
import org.elm.ide.typing.guessIndent
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmAnythingPattern
import org.elm.lang.core.psi.elements.ElmCaseOfExpr
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.stubs.index.ElmNamedElementIndex
import org.elm.lang.core.types.*

/**
 * This class can detect missing branches for case expressions and insert them into the PSI in place.
 */
class MissingCaseBranchAdder(val element: ElmCaseOfExpr) {
    // This should be a lazy {}, but using it causes a compilation error due to conflicting
    // declarations.
    val missingBranches: VariantParameters
        get() {
            if (_missingBranches == null) _missingBranches = calcMissingBranches()
            return _missingBranches!!
        }
    private var _missingBranches: VariantParameters? = null

    fun addMissingBranches() {
        if (missingBranches.isEmpty()) return

        val patterns = missingBranches.map { (name, params) ->
            (listOf(name) + params.map { it.renderParam() }).joinToString(" ")
        }

        addPatterns(patterns)
    }

    fun addWildcardBranch() {
        if (missingBranches.isEmpty()) return
        addPatterns(listOf("_"))
    }

    private fun addPatterns(patterns: List<String>) {
        val factory = ElmPsiFactory(element.project)
        val existingBranches = element.branches
        val indent = guessIndent(element) + "    "
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

    private fun calcMissingBranches(): VariantParameters {
        val inference = element.findInference() ?: return emptyMap()

        // This only works on case expressions with a union type for now
        val exprTy = element.expression?.let { inference.elementType(it) } as? TyUnion
                ?: return emptyMap()

        val project = element.project
        val declaration = ElmNamedElementIndex.find(exprTy.name, project, GlobalSearchScope.allScope(project))
                .filterIsInstance<ElmTypeDeclaration>()
                .find { it.moduleName == exprTy.module }
                ?: return emptyMap()
        val allBranches = declaration.variantInference().value
        val missingBranches = allBranches.toMutableMap()

        for (branch in element.branches) {
            val pat = branch.pattern.child
            when (pat) {
                is ElmAnythingPattern -> return emptyMap() // covers all cases
                is ElmUnionPattern -> {
                    missingBranches.remove(pat.referenceName)
                }
                else -> return emptyMap() // invalid pattern
            }
        }

        return missingBranches
    }
}
