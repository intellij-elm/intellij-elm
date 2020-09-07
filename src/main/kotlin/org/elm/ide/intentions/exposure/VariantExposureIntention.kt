package org.elm.ide.intentions.exposure

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * Abstract base class for intentions which toggle whether a type which is exposed from a module also exposes its
 * variants, i.e. toggling between `exposing (MyType)` and `exposing (MyType(..))`.
 */
abstract class VariantExposureIntention : ExposureIntentionBase<VariantExposureIntention.Context>() {

    data class Context(val exposedType: ElmExposedType)

    /**
     * Indicates whether `this` [ElmExposedType] is valid for handling using this intention.
     */
    protected abstract val ElmExposedType.isValidForReplacement: Boolean

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val (exposedItem) = getExposedTypeAt(element) ?: return null
        if (exposedItem !is ElmExposedType) return null
        // Make sure we're working with a custom type, not a type alias.
        if (exposedItem.reference.resolve() !is ElmTypeDeclaration) return null
        if (!exposedItem.isValidForReplacement) return null

        return Context(exposedItem)
    }
}
