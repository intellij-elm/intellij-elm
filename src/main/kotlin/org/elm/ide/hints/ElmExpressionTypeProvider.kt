package org.elm.ide.hints

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmParenthesizedExpr
import org.elm.lang.core.types.findInference
import org.elm.lang.core.types.findTy
import org.elm.lang.core.types.renderedText

/**
 * Provides the text for the "Expression Type" command, which is typically bound to `Ctrl+Shift+P` or `Cmd+Shift+P`
 */
class ElmExpressionTypeProvider : ExpressionTypeProvider<PsiElement>() {
    override fun getErrorHint() = "Type is unknown"

    override fun getInformationHint(element: PsiElement): String {
        val ty = (element as? ElmPsiElement)?.findTy() ?: return errorHint
        return StringUtil.escapeXmlEntities(ty.renderedText())
    }

    override fun getExpressionsAt(elementAt: PsiElement): List<PsiElement> {
        val expressionTypes = elementAt.findInference()?.expressionTypes ?: return emptyList()
        return elementAt.ancestors.takeWhile { it !is ElmFile }
                .filter { it in expressionTypes && it !is ElmParenthesizedExpr }
                .toList()
    }
}
