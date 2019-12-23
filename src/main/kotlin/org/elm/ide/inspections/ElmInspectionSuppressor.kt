package org.elm.ide.inspections

import com.intellij.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.codeInspection.SuppressionUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_DECL
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmValueDeclaration

class ElmInspectionSuppressor : InspectionSuppressor {
    companion object {
        private val SUPPRESS_REGEX = Regex("--" + SuppressionUtil.COMMON_SUPPRESS_REGEXP)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> = arrayOf(
            SuppressInspectionFix(toolId),
            SuppressInspectionFix(SuppressionUtil.ALL)
    )

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean =
            element.ancestors.filterIsInstance<ElmPsiElement>().firstOrNull { it.isTopLevel }
                    ?.isSuppressedByComment(toolId)
                    ?: false

    private fun ElmPsiElement.isSuppressedByComment(toolId: String): Boolean {
        return prevSiblings.takeWhile {
            it is PsiWhiteSpace ||
                    it is PsiComment ||
                    it.elementType == VIRTUAL_END_DECL ||
                    this is ElmValueDeclaration && it is ElmTypeAnnotation
        }.filterIsInstance<PsiComment>().any { comment ->
            val match = SUPPRESS_REGEX.matchEntire(comment.text)
            match != null && SuppressionUtil.isInspectionToolIdMentioned(match.groupValues[1], toolId)
        }
    }

    private class SuppressInspectionFix(
            id: String
    ) : AbstractBatchSuppressByNoInspectionCommentFix(id, /* replaceOthers = */ id == SuppressionUtil.ALL) {

        init {
            text = when (id) {
                SuppressionUtil.ALL -> "Suppress all inspections for declaration"
                else -> "Suppress for declaration with comment"
            }
        }

        override fun getContainer(context: PsiElement?): PsiElement? {
            if (context == null) return null
            return context.ancestors.filterIsInstance<ElmPsiElement>().firstOrNull { it.isTopLevel }
        }

        override fun createSuppression(project: Project, element: PsiElement, container: PsiElement) {
            val anchor = (container as? ElmValueDeclaration)?.typeAnnotation ?: container
            val text = SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME + " " + myID
            val comment = SuppressionUtil.createComment(project, text + "\n", ElmLanguage)
            val parent = anchor.parent
            parent.addBefore(comment, anchor)
            parent.addBefore(ElmPsiFactory(element.project).createWhitespace("\n"), anchor)
        }
    }
}
