package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.diagnostics.ElmDiagnostic

abstract class ElmDiagnosticBasedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement?) {
            super.visitElement(element)
            if (element != null) {
                getElementDiagnostics(element)
                for (diagnostic in getElementDiagnostics(element)) {
                    holder.registerProblem(holder.manager.createProblemDescriptor(
                            diagnostic.element,
                            diagnostic.endElement ?: diagnostic.element,
                            "<html>${diagnostic.message}</html>",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            holder.isOnTheFly
                    ))
                }
            }
        }
    }

    abstract fun getElementDiagnostics(element: PsiElement): Iterable<ElmDiagnostic>
}
