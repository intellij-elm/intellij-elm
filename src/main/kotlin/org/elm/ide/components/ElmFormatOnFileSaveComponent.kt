package org.elm.ide.components

import com.intellij.AppTopics
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.isElmFile
import org.elm.openapiext.isUnitTestMode
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.elmSettings
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace

class ElmFormatOnFileSaveComponent(val project: Project) : ProjectComponent {

    override fun initComponent() {
        val application = ApplicationManager.getApplication()
        val bus = application.messageBus

        bus.connect(project).subscribe(
                AppTopics.FILE_DOCUMENT_SYNC,
                object : FileDocumentManagerListener {
                    override fun beforeDocumentSaving(document: Document) {

                        if (!project.elmSettings.toolchain.isElmFormatOnSaveEnabled) return

                        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
                        if (PsiTreeUtil.hasErrorElements(psiFile)) return

                        val vFile = FileDocumentManager.getInstance().getFile(document) ?: return
                        if (!vFile.isElmFile) return

                        val elmVersion = ElmFormatCLI.getElmVersion(project, vFile) ?: return
                        val elmFormat = project.elmToolchain.elmFormatCLI ?: return

                        try {
                            elmFormat.formatDocumentAndSetText(project, document, elmVersion, addToUndoStack = false)
                        } catch (e: ExecutionException) {
                            if (isUnitTestMode) throw e
                            val message = e.message ?: "Something went wrong running elm-format"
                            val actions = when (e) {
                                is ProcessNotCreatedException ->
                                    arrayOf("Fix Path" to { project.elmWorkspace.showConfigureToolchainUI() })
                                else -> emptyArray()
                            }
                            project.showBalloon(message, NotificationType.ERROR, *actions)
                        }
                    }
                }
        )
    }
}