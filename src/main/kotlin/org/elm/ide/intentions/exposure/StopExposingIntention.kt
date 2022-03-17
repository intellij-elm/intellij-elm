package org.elm.ide.intentions.exposure

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.removeItem

/**
 * An intention action that removes a function from a module's `exposing` list.
 */
open class StopExposingIntention : ExposureIntentionBase<StopExposingIntention.Context>() {

    data class Context(val element: ElmExposedItemTag, val exposingList: ElmExposingList)

    override fun getText() = "Stop exposing"

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement) =
        getExposedTypeAt(element)?.let { createContext(it.exposedItem, it.exposingList) }

    /**
     * Creates a [Context] based on the passed in parameters. Overriding subclasses can return null if they find that
     * the passed in [decl] isn't valid for their particular intention.
     */
    protected open fun createContext(decl: ElmExposedItemTag, exposingList: ElmExposingList): Context? =
        Context(decl, exposingList)

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            context.exposingList.removeItem(context.element)
        }
    }
}
