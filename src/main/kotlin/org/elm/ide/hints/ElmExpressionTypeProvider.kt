package org.elm.ide.hints

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.types.inference
import org.elm.lang.core.types.renderedText

/**
 * Provides the text for the "Expression Type" command, which is typically bound to `Ctrl+Shift+P` or `Cmd+Shift+P`
 */
class ElmExpressionTypeProvider : ExpressionTypeProvider<PsiElement>() {
    // TODO update once inference is in place

    override fun getErrorHint() = "Type is unknown"

    override fun getInformationHint(element: PsiElement): String {
        val ref = element.parentOfType<ElmValueExpr>()?.reference?.resolve() ?: return errorHint
        val inference = element.parentOfType<ElmValueDeclaration>()?.inference() ?: return errorHint
        return StringUtil.escapeXml(inference.bindingType(ref).renderedText(false, false))
    }

    override fun getExpressionsAt(elementAt: PsiElement): List<PsiElement> {
        return if (elementAt.elementType == LOWER_CASE_IDENTIFIER) listOf(elementAt) else emptyList() // TODO inferred types
    }
}
