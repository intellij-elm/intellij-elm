package org.elm.ide.lineMarkers

import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.*

/**
 * Put an icon in the gutter for top-level declarations (types, functions, values)
 * that are exposed by the containing module.
 */
class ElmExposureLineMarkerProvider : LineMarkerProvider {
    companion object {
        val OPTION = GutterIconDescriptor.Option("elm.exposed", "Exposed declaration", ElmIcons.EXPOSED_GUTTER)
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element.elementType !in listOf(LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return null

        val parentDecl = element.parent
        if (parentDecl !is ElmNameIdentifierOwner || parentDecl.nameIdentifier != element) return null

        return when (parentDecl) {
            is ElmUnionVariant ->
                null

            is ElmFunctionDeclarationLeft ->
                if (!parentDecl.isTopLevel) null
                else makeMarkerIfExposed(element, parentDecl)

            is ElmExposableTag ->
                makeMarkerIfExposed(element, parentDecl)

            else ->
                null
        }
    }
}


private fun makeMarkerIfExposed(element: PsiElement, decl: ElmExposableTag): LineMarkerInfo<PsiElement>? {
    val elmFile = element.containingFile as? ElmFile ?: return null
    val moduleDecl = elmFile.getModuleDecl() ?: return null
    val exposingList = moduleDecl.exposingList ?: return null
    val isAtCustomType = element.parent is ElmTypeDeclaration

    // If exposing all, i.e. `module Foo exposing (..)`, then isTypeWithExposedVariants is true if this is a custom type
    // since `exposing(..)` implies a custom type's variants are exposed. Otherwise, check the actual exposure to see if
    // it explicitly exposes the variants, e.g. `module Foo exposing (MyType(..))`.
    if (moduleDecl.exposesAll) {
        return makeMarker(element, exposingList.doubleDot, isAtCustomType)
    } else {
        val exposedItem = exposingList.findMatchingItemFor(decl) ?: return null
        return makeMarker(element, exposedItem, isAtCustomType && (exposedItem as? ElmExposedType)?.exposesAll == true)
    }
}


private fun makeMarker(element: PsiElement, target: PsiElement?, isTypeWithExposedVariants: Boolean) =
    NavigationGutterIconBuilder
        .create(ElmIcons.EXPOSED_GUTTER)
        .setTarget(target)
        .setPopupTitle("Go to where it is exposed")
        .setTooltipText(if (isTypeWithExposedVariants) "Exposed (including variants)" else "Exposed")
        .createLineMarkerInfo(element)
