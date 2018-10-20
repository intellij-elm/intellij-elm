package org.elm.ide.hints

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.inference
import org.elm.lang.core.types.renderedText

/**
 * Provides the text for the "Expression Type" command, which is typically bound to `Ctrl+Shift+P` or `Cmd+Shift+P`
 */
class ElmExpressionTypeProvider : ExpressionTypeProvider<PsiElement>() {
    override fun getErrorHint() = "Type is unknown"

    override fun getInformationHint(element: PsiElement): String {
        val ty = getInferedTypes(element)?.get(element) ?: return errorHint
        return StringUtil.escapeXml(ty.renderedText(false, false))
    }

    override fun getExpressionsAt(elementAt: PsiElement): List<PsiElement> {
        val elementTypes = getInferedTypes(elementAt) ?: return emptyList()
        return elementAt.ancestors.takeWhile { it !is ElmFile }.filter { it in elementTypes }.toList()
    }

    private fun getInferedTypes(element: PsiElement): Map<ElmPsiElement, Ty>? {
        return element.ancestors.takeWhile { it !is ElmFile }
                .filterIsInstance<ElmValueDeclaration>()
                .firstOrNull { it.isTopLevel }
                ?.inference()?.elementTypes
    }
}
