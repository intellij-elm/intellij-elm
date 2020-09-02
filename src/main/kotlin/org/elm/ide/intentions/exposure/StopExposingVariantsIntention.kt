package org.elm.ide.intentions.exposure

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.elm.lang.core.psi.elements.ElmExposedType

/**
 * For a custom type which is already exposed, but its variants aren't, this removes the variants from the exposure,
 * i.e. changes `exposing (MyType(..))` to `exposing (MyType)`.
 */
class StopExposingVariantsIntention : VariantExposureIntention() {

    override fun getText() = "Stop exposing variants"

    override val ElmExposedType.isValidForReplacement: Boolean
        get() = this.exposesAll

    override fun invoke(project: Project, editor: Editor, context: Context) {
        context.exposedType.deleteParensAndDoubleDot()
    }
}
