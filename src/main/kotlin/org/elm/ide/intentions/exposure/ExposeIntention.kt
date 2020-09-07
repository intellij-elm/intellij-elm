package org.elm.ide.intentions.exposure

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that adds a function to a module's `exposing` list.
 */
open class ExposeIntention : ExposureIntentionBase<ExposeIntention.Context>() {

    data class Context(val nameToExpose: String, val exposingList: ElmExposingList)

    override fun getText() = "Expose"

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val exposingList = getExposingList(element) ?: return null

        // check if the caret is on the identifier that names the exposable declaration
        val decl = element.parent as? ElmExposableTag ?: return null
        if (decl.nameIdentifier != element) return null

        return when {
            decl is ElmUnionVariant -> {
                // might be nice to support this in the future (making a union type fully exposed)
                null
            }

            decl is ElmFunctionDeclarationLeft && !decl.isTopLevel ->
                null

            !exposingList.exposes(decl) ->
                createContext(decl, exposingList)

            else ->
                null
        }
    }

    /**
     * Creates a [Context] based on the passed in parameters. Overriding subclasses can return null if they find that
     * the passed in [decl] isn't valid for their particular intention.
     */
    protected open fun createContext(decl: ElmExposableTag, exposingList: ElmExposingList): Context? =
        Context(decl.name, exposingList)

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            context.exposingList.addItem(context.nameToExpose)
        }
    }
}
