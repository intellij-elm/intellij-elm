package org.elm.ide.inspections

import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmAnythingPattern
import org.elm.lang.core.psi.elements.ElmCaseOfExpr
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.findInference
import org.elm.lang.core.types.renderParam

/**
 * This class can detect missing branches for case expressions and insert them into the PSI in place.
 */
class MissingCaseBranchAdder(val element: ElmCaseOfExpr) {
    // This should be a lazy {}, but using it causes a compilation error due to conflicting
    // declarations.
    val missingBranches: List<TyUnion.Member>
        get() {
            if (_missingBranches == null) _missingBranches = calcMissingBranches()
            return _missingBranches!!
        }
    private var _missingBranches: List<TyUnion.Member>? = null

    fun addMissingBranches() {
        if (missingBranches.isEmpty()) return

        val patterns = missingBranches.map { b ->
            (listOf(b.name) + b.parameters.map { it.renderParam() }).joinToString(" ")
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
        val indent = "    ".repeat(element.ancestors.takeWhile { it !is ElmFile }.count())
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

    private fun calcMissingBranches(): List<TyUnion.Member> {
        val inference = element.findInference() ?: return emptyList()

        // This only works on case expressions with a union type for now
        val exprTy = element.expression?.let { inference.elementType(it) } as? TyUnion
                ?: return emptyList()

        val allBranches = exprTy.members.associateBy { it.name }
        val missingBranches = allBranches.toMutableMap()

        for (branch in element.branches) {
            val pat = branch.pattern.child
            when (pat) {
                is ElmAnythingPattern -> return emptyList() // covers all cases
                is ElmUnionPattern -> {
                    missingBranches.remove(pat.referenceName)
                }
                else -> return emptyList() // invalid pattern
            }
        }

        return missingBranches.values.toList()
    }
}
