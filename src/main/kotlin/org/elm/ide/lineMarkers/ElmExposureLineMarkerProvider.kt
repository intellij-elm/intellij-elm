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
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * Put an icon in the gutter for top-level declarations (types, functions, values)
 * that are exposed by the containing module.
 */
class ElmExposureLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element.elementType !in listOf(LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return null
        val elmFile = element.containingFile as? ElmFile ?: return null
        val moduleDecl = elmFile.getModuleDecl() ?: return null

        val parent = element.parent
        if (parent !is ElmNameIdentifierOwner || parent.nameIdentifier != element) return null

        when (parent) {
            is ElmFunctionDeclarationLeft ->
                if (moduleDecl.exposesFunction(parent))
                    return makeMarker(element)

            is ElmTypeDeclaration,
            is ElmTypeAliasDeclaration ->
                if (moduleDecl.exposesType(parent))
                    return makeMarker(element)
        }
        return null
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        // we don't need to add anything here
    }
}


private fun makeMarker(element: PsiElement): LineMarkerInfo<PsiElement> {
    return LineMarkerInfo(
            element,
            element.textRange,
            ElmIcons.EXPOSED_GUTTER,
            Pass.LINE_MARKERS,
            { "Exposed" },
            { _, _ -> Unit },
            GutterIconRenderer.Alignment.RIGHT
    )
}


private fun ElmModuleDeclaration.exposesFunction(decl: ElmFunctionDeclarationLeft): Boolean {
    if (!decl.isTopLevel) return false
    if (exposesAll) return true
    val exposedValues = exposingList?.exposedValueList ?: return false
    return exposedValues.any { it.reference.isReferenceTo(decl) }
}


private fun ElmModuleDeclaration.exposesType(decl: PsiElement): Boolean {
    if (exposesAll) return true
    val exposedTypes = exposingList?.exposedTypeList ?: return false
    return exposedTypes.any { it.reference.isReferenceTo(decl) }
}
