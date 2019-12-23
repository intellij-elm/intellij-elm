package org.elm.ide.lineMarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.FunctionUtil
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement

/**
 * Put an icon in the gutter for top-level declarations (types, functions, values)
 * that are exposed by the containing module.
 */
class ElmRecursiveCallLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
            elements: List<PsiElement>,
            result: MutableCollection<LineMarkerInfo<PsiElement>>
    ) {
        // we use the slow pass since we need to resolve references

        val lines = mutableSetOf<Int>() // Only one marker per line

        for (el in elements) {
            if (el.elementType != LOWER_CASE_IDENTIFIER) continue
            val qid = el.parent as? ElmValueQID ?: continue
            if (qid.qualifiers.isNotEmpty()) continue
            val valueExpr = qid.parent as? ElmValueExpr ?: continue
            val functionCall = valueExpr.parent as? ElmFunctionCallExpr ?: continue
            if (functionCall.target != valueExpr) continue

            val ref = valueExpr.reference.resolve()
            val nearestFunc = functionCall.ancestorsStrict.filterIsInstance<ElmValueDeclaration>()
                    .firstOrNull()?.functionDeclarationLeft
            if (nearestFunc != ref) continue // not recursive

            val doc = PsiDocumentManager.getInstance(el.project).getDocument(el.containingFile) ?: continue
            val lineNumber = doc.getLineNumber(el.textOffset)
            if (lines.add(lineNumber)) {
                result.add(LineMarkerInfo(
                        el,
                        el.textRange,
                        ElmIcons.RECURSIVE_CALL,
                        FunctionUtil.constant("Recursive call"),
                        null,
                        GutterIconRenderer.Alignment.RIGHT)
                )
            }
        }
    }
}
