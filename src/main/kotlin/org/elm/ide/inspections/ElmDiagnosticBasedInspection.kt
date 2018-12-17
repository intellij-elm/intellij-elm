package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.ElmPsiElement

abstract class ElmDiagnosticBasedInspection : ElmLocalInspection() {
    final override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
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

    abstract fun getElementDiagnostics(element: ElmPsiElement): Iterable<ElmDiagnostic>
}

