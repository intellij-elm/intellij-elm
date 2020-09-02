package org.elm.ide.intentions.exposure

import com.intellij.psi.PsiElement
import org.elm.ide.intentions.ElmAtCaretIntentionActionBase
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmUnionVariant
import org.elm.lang.core.psi.elements.findMatchingItemFor

/**
 * Abstract base class for all intentions which work with a module's `exposing` list.
 */
abstract class ExposureIntentionBase<Ctx> : ElmAtCaretIntentionActionBase<Ctx>() {

    /**
     * Information about an exposed item, namely the [exposedItem] itself and the [exposingList] which exposes it.
     */
    protected data class ExposedItemInfo(val exposedItem: ElmExposedItemTag, val exposingList: ElmExposingList)

    override fun getFamilyName() = text

    /**
     * Gets the [ElmExposingList] for the file containing [element], or null if there is none.
     */
    protected fun getExposingList(element: PsiElement) =
        (element.containingFile as? ElmFile)?.getModuleDecl()?.exposingList

    /**
     * Gets the [ExposedItemInfo] corresponding to [element], or null if it doesn't correspond to an exposed item.
     * In other words, for the given [element], if it is at a type which is individually exposed, this returns
     * information about that exposure.
     */
    protected fun getExposedTypeAt(element: PsiElement): ExposedItemInfo? {
        val exposingList = getExposingList(element) ?: return null

        if (exposingList.allExposedItems.size == 1) {
            // Elm's exposing list can never be empty. In this case, there is only one thing left
            // in the list, and if we were to remove it, the list would become empty. So we will
            // return null to indicate that the user cannot hide the current thing.
            return null
        }

        // check if the caret is on the identifier that names the exposable declaration
        val decl = element.parent as? ElmExposableTag ?: return null
        if (decl.nameIdentifier != element) return null

        return if (decl is ElmUnionVariant) {
            // might be nice to support this in the future (making a union type opaque)
            null
        } else {
            exposingList.findMatchingItemFor(decl)
                ?.let { ExposedItemInfo(it, exposingList) }
        }
    }
}
