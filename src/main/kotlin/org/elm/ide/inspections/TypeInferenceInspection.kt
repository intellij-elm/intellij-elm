package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmFieldAccess
import org.elm.lang.core.psi.elements.ElmLetIn
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.types.inference

class TypeInferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement?) {
            super.visitElement(element)
            if (element is ElmValueDeclaration) {
                // nested declarations are taken care of in the parent inference
                if (!element.isTopLevel) return

                val inference = element.inference()
                for (diagnostic in inference.diagnostics) {
                    // TODO This seems to cause the InspectionDescriptionLinkHandler to add the text
                    // "Under construction" to the description.
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
}
