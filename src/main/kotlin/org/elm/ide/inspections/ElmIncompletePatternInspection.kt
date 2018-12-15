package org.elm.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmCaseOfExpr

class ElmIncompletePatternInspection : ElmLocalInspection() {
    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmCaseOfExpr) return
        val fixer = MissingCaseBranchAdder(element)
        if (fixer.missingBranches.isEmpty()) return

        holder.registerProblem(element.firstChild,
                "Case expression is not exhaustive",
                object : LocalQuickFix {
                    override fun getName(): String = "Add missing case branches"
                    override fun getFamilyName(): String = name
                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        fixer.addMissingBranches()
                    }
                })
    }
}
