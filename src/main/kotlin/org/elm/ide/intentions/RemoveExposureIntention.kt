package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
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

        val parent = element.parent ?: return null

        if (element.elementType == ElmTypes.LOWER_CASE_IDENTIFIER
                && parent is ElmFunctionDeclarationLeft
                && parent.lowerCaseIdentifier == element) {
            return exposingList.exposesFunction(parent)?.let { Context(it, exposingList) }
        }

        return null
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        object : WriteCommandAction.Simple<Unit>(project) {
            override fun run() {
                context.exposingList.removeItem(context.element)
            }
        }.execute()
    }
}


private fun ElmExposingList.exposesFunction(decl: ElmFunctionDeclarationLeft): ElmExposedValue? {
    if (!decl.isTopLevel) return null
    return exposedValueList.find { it.reference.isReferenceTo(decl) }
}