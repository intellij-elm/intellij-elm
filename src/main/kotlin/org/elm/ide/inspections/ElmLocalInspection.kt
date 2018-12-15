package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
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
}
