package org.elm.ide.inspections

import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement

abstract class ElmLocalInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement?) {
            super.visitElement(element)
            if (element is ElmPsiElement) {
                visitElement(element, holder, isOnTheFly)
            }
        }
    }

    abstract fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean)

    protected inline fun quickFix(
            name: String,
            familyName: String = name,
            priority: PriorityAction.Priority = PriorityAction.Priority.NORMAL,
            crossinline fix: (Project, ProblemDescriptor) -> Unit
    ): LocalQuickFix = object : LocalQuickFix, PriorityAction {
        override fun getName(): String = name
        override fun getFamilyName(): String = familyName
        override fun getPriority(): PriorityAction.Priority = priority
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            fix(project, descriptor)
        }
    }
}
