package org.elm.ide

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.runWriteCommandAction

class ElmTypedHandler : TypedHandlerDelegate() {
    override fun beforeSelectionRemoved(c: Char, project: Project, editor: Editor, psiFile: PsiFile): Result {
        if (psiFile !is ElmFile) return super.charTyped(c, project, editor, psiFile)

        val charMap = mapOf('(' to ')', '{' to '}', '[' to ']')

        if (editor.selectionModel.hasSelection()
            && charMap.contains(c)
            && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            project.runWriteCommandAction {
                editor.document.insertString(editor.selectionModel.selectionStart, c.toString())
                editor.document.insertString(editor.selectionModel.selectionEnd, charMap[c].toString())
            }
            return Result.STOP
        }

        return Result.CONTINUE
    }
}