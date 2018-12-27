package org.elm.ide.lineMarkers

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.findMatchingItemFor
import java.awt.event.MouseEvent

/**
 * Put an icon in the gutter for top-level declarations (types, functions, values)
 * that are exposed by the containing module.
 */
class ElmExposureLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element.elementType !in listOf(LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return null

        val parent = element.parent
        if (parent !is ElmNameIdentifierOwner || parent.nameIdentifier != element) return null

        return when (parent) {
            is ElmFunctionDeclarationLeft ->
                if (!parent.isTopLevel) null
                else makeMarkerIfNecessary(element, parent)

            is ElmTypeDeclaration,
            is ElmTypeAliasDeclaration ->
                makeMarkerIfNecessary(element, parent)

            else ->
                null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        // we don't need to add anything here
    }
}


private fun makeMarkerIfNecessary(element: PsiElement, decl: ElmNameIdentifierOwner): LineMarkerInfo<PsiElement>? {
    val elmFile = element.containingFile as? ElmFile ?: return null
    val moduleDecl = elmFile.getModuleDecl() ?: return null
    val exposingList = moduleDecl.exposingList ?: return null
    if (moduleDecl.exposesAll) {
        return makeMarker(element) { _, _ -> Unit }
    } else {
        val item = exposingList.findMatchingItemFor(decl) ?: return null
        return makeMarker(element) { _, _ -> Unit }
    }
}


private fun makeMarker(element: PsiElement, navHandler: (MouseEvent, PsiElement) -> Unit) =
        LineMarkerInfo(
                element,
                element.textRange,
                ElmIcons.EXPOSED_GUTTER,
                Pass.LINE_MARKERS,
                { "Exposed" },
                navHandler,
                GutterIconRenderer.Alignment.RIGHT
        )
