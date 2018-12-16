package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmCaseOfExpr

class ElmIncompletePatternInspection : ElmLocalInspection() {
    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmCaseOfExpr) return
        val fixer = MissingCaseBranchAdder(element)
        if (fixer.missingBranches.isEmpty()) return

        holder.registerProblem(element.firstChild,
                "Case expression is not exhaustive",
                quickFix("Add missing case branches") { _, _ -> fixer.addMissingBranches() },
                quickFix("Add '_' branch") { _, _ -> fixer.addWildcardBranch() }
        )
    }
}
