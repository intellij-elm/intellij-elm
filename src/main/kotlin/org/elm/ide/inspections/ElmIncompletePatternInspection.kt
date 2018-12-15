package org.elm.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.TokenType.WHITE_SPACE
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes.VIRTUAL_OPEN_SECTION
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmAnythingPattern
import org.elm.lang.core.psi.elements.ElmCaseOfExpr
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.findInference
import org.elm.lang.core.types.renderedText

class ElmIncompletePatternInspection : ElmLocalInspection() {
    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmCaseOfExpr) return
        val inference = element.findInference() ?: return

        // This expression only applies to union types for now
        val exprTy = element.expression?.let { inference.elementType(it) } as? TyUnion ?: return
        val allBranches = exprTy.members.associateBy { it.name }
        val missingBranches = allBranches.toMutableMap()

        for (branch in element.branches) {
            val pat = branch.pattern.child
            when (pat) {
                is ElmAnythingPattern -> return // covers all cases
                is ElmUnionPattern -> {
                    missingBranches.remove(pat.referenceName)
                }
                else -> return // invalid pattern
            }
        }

        if (missingBranches.isEmpty()) return
        holder.registerProblem(element.firstChild,
                "Case expression is not exhaustive",
                addMissingBranchesQuickFix(element, missingBranches.values))
    }

    private fun addMissingBranchesQuickFix(
            caseOf: ElmCaseOfExpr,
            branches: Collection<TyUnion.Member>
    ) = object : LocalQuickFix {
        override fun getName(): String = "Add missing case branches"
        override fun getFamilyName(): String = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val factory = ElmPsiFactory(caseOf.project)
            val patterns = branches.map { b ->
                b.name + b.parameters.joinToString(" ") { it.renderedText(false, false).toLowerCase() }
            }
            val existingBranches = caseOf.branches
            val insertLocation = when {
                existingBranches.isEmpty() -> caseOf
                else -> existingBranches.last()
            }

            val ws = insertLocation.prevSibling
            val indent = when (ws?.elementType) {
                WHITE_SPACE, VIRTUAL_OPEN_SECTION -> ws.text
                else -> "    "
            } + if (existingBranches.isEmpty()) "    " else ""

            val elements = factory.createCaseOfBranches(indent, patterns)
            // Add the two or three elements before the first generated branch, which are the indent
            // and newlines
            var start = elements.first().prevSibling.prevSibling

            if (existingBranches.isNotEmpty()) {
                start = start.prevSibling
            }

            caseOf.addRangeAfter(start, elements.last(), insertLocation)
        }
    }
}
