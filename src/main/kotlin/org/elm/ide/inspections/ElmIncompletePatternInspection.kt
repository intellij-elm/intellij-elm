package org.elm.ide.inspections

import com.intellij.codeInsight.intention.PriorityAction.Priority.LOW
import com.intellij.codeInspection.ProblemsHolder
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmCaseOfExpr

class ElmIncompletePatternInspection : ElmLocalInspection() {
    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmCaseOfExpr) return
        val fixer = MissingCaseBranchAdder(element)

        if (fixer.result == MissingCaseBranchAdder.Result.NoMissing) return

        val extraFixes = if (fixer.result is MissingCaseBranchAdder.Result.MissingVariants) {
            arrayOf(quickFix("Add missing case branches") { _, _ -> fixer.addMissingBranches() })
        } else emptyArray()

        val fixes = extraFixes + arrayOf(quickFix("Add '_' branch", priority = LOW) { _, _ -> fixer.addWildcardBranch() })

        holder.registerProblem(element.firstChild,
                "Case expression is not exhaustive",
                *fixes
        )
    }
}
