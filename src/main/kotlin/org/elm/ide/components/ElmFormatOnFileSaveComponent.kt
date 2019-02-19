package org.elm.ide.components

import com.intellij.AppTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.ElmFormatCLI
import org.elm.workspace.elmSettings
import org.elm.workspace.elmToolchain

class ElmFormatOnFileSaveComponent(val project: Project) : ProjectComponent {

    override fun initComponent() {
        val application = ApplicationManager.getApplication()
        val bus = application.messageBus

        bus.connect(project).subscribe(
                AppTopics.FILE_DOCUMENT_SYNC,
                object : FileDocumentManagerListener {
                    override fun beforeDocumentSaving(document: Document) {

                        if (project.elmSettings.toolchain?.isElmFormatOnSaveEnabled != true) return

                        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
                        if (PsiTreeUtil.hasErrorElements(psiFile)) return

                        val vFile = FileDocumentManager.getInstance().getFile(document) ?: return
                        if (!vFile.isElmFile) return

                        val elmVersion = ElmFormatCLI.getElmVersion(project, vFile) ?: return
                        val elmFormat = project.elmToolchain?.elmFormat ?: return

                        elmFormat.formatDocumentAndSetText(project, document, elmVersion, addToUndoStack = false)
                    }
                }
        )
    }
}