package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmUnionVariant
import org.elm.lang.core.psi.elements.findMatchingItemFor
import org.elm.lang.core.psi.elements.removeItem

/**
 * An intention action that removes a function/type from a module's `exposing` list.
 */
class RemoveExposureIntention : ElmAtCaretIntentionActionBase<RemoveExposureIntention.Context>() {

    data class Context(val element: ElmExposedItemTag, val exposingList: ElmExposingList)

    override fun getText() = "Remove from exposing list"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val elmFile = element.containingFile as? ElmFile ?: return null
        val moduleDecl = elmFile.getModuleDecl() ?: return null
        val exposingList = moduleDecl.exposingList ?: return null

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
                    ?.let { Context(it, exposingList) }
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            context.exposingList.removeItem(context.element)
        }
    }
}
