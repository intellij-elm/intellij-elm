package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNameIdentifierOwner
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

        if (exposingList.doubleDot != null) {
            // The module already exposes everything. Nothing to do here.
            return null
        }

        val parent = element.parent as? ElmNameIdentifierOwner ?: return null

        if (parent.nameIdentifier != element) return null

        return when (parent) {
            is ElmFunctionDeclarationLeft,
            is ElmTypeDeclaration,
            is ElmTypeAliasDeclaration ->
                if (exposingList.allExposedItems.none { it.reference?.isReferenceTo(parent) ?: false }) {
                    Context(parent.name, exposingList)
                } else {
                    // already exposed
                    null
                }
            else -> null
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
