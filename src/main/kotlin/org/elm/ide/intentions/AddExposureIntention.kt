package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that adds a function/type to a module's `exposing` list.
 */
class AddExposureIntention : ElmAtCaretIntentionActionBase<AddExposureIntention.Context>() {

    data class Context(val nameToExpose: String, val exposingList: ElmExposingList)

    override fun getText() = "Add to exposing list"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val exposingList = (element.containingFile as? ElmFile)?.getModuleDecl()?.exposingList
                ?: return null

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
                Context(decl.name, exposingList)

            else ->
                null
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        object : WriteCommandAction.Simple<Unit>(project) {
            override fun run() {
                context.exposingList.addItem(context.nameToExpose)
            }
        }.execute()
    }
}
