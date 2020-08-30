package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

/**
 * Find unused functions, parameters, etc.
 */
class ElmUnusedLeftPipeInspection : ElmLocalInspection() {

    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmOperator) return
        if (element.referenceName != "<|") return
        val rightOperand = element.nextSiblings.withoutWsOrComments.firstOrNull() ?: return
        when (rightOperand) {
            is ElmParenthesizedExpr, is ElmListExpr, is ElmRecordExpr -> {
                holder.registerProblem(
                        element,
                        "'<|' is not necessary",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        DeleteLeftPipeFix()
                )
            }
        }
    }

    companion object {
        val fixName = "Safely Delete <|"
    }
}

private class DeleteLeftPipeFix : NamedQuickFix(ElmUnusedLeftPipeInspection.fixName) {
    override fun applyFix(element: PsiElement, project: Project) {
        val spaces = (element.nextSibling as? PsiWhiteSpace)
                ?.takeIf { '\n' !in it.text }
        when (spaces) {
            null -> element.delete()
            else -> element.parent.deleteChildRange(element, spaces)
        }
    }
}