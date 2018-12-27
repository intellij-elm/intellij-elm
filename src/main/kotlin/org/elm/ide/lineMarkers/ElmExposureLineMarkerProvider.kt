package org.elm.ide.lineMarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
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

/**
 * Put an icon in the gutter for top-level declarations (types, functions, values)
 * that are exposed by the containing module.
 */
class ElmExposureLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element.elementType !in listOf(LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return null

        val parentDecl = element.parent
        if (parentDecl !is ElmNameIdentifierOwner || parentDecl.nameIdentifier != element) return null

        return when (parentDecl) {
            is ElmFunctionDeclarationLeft ->
                if (!parentDecl.isTopLevel) null
                else makeMarkerIfExposed(element, parentDecl)

            is ElmTypeDeclaration,
            is ElmTypeAliasDeclaration ->
                makeMarkerIfExposed(element, parentDecl)

            else ->
                null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        // we don't need to add anything here
    }
}


private fun makeMarkerIfExposed(element: PsiElement, decl: ElmNameIdentifierOwner): LineMarkerInfo<PsiElement>? {
    val elmFile = element.containingFile as? ElmFile ?: return null
    val moduleDecl = elmFile.getModuleDecl() ?: return null
    val exposingList = moduleDecl.exposingList ?: return null
    if (moduleDecl.exposesAll) {
        return makeMarker(element, exposingList.doubleDot)
    } else {
        val item = exposingList.findMatchingItemFor(decl) ?: return null
        return makeMarker(element, item)
    }
}


private fun makeMarker(element: PsiElement, target: PsiElement?) =
        NavigationGutterIconBuilder
                .create(ElmIcons.EXPOSED_GUTTER)
                .setTarget(target)
                .setPopupTitle("Go to where it is exposed")
                .setTooltipText("Exposed")
                .createLineMarkerInfo(element)
