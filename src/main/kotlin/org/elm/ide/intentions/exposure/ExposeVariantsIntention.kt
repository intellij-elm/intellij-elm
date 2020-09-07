package org.elm.ide.intentions.exposure

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmExposedType

/**
 * For a custom type which is already exposed, but whose variants aren't, this exposes the variants, i.e. changes
 * `exposing (MyType)` to `exposing (MyType(..))`.
 */
class ExposeVariantsIntention : VariantExposureIntention() {

    override fun getText() = "Expose variants"

    override val ElmExposedType.isValidForReplacement: Boolean
        get() = !this.exposesAll

    override fun invoke(project: Project, editor: Editor, context: Context) {
        context.exposedType.replace(
            ElmPsiFactory(project).createTypeWithVariantsExposure(context.exposedType.referenceName)
        )
    }
}
