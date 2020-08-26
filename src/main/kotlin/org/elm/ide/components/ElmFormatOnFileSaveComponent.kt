package org.elm.ide.components

import com.intellij.AppTopics
import com.intellij.ide.DataManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import org.elm.ide.notifications.executeAction
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI.ElmFormatResult
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
                        val vFile = FileDocumentManager.getInstance().getFile(document) ?: return
                        if (!vFile.isElmFile) return
                        val elmVersion = ElmFormatCLI.getElmVersion(project, vFile) ?: return
                        val elmFormat = project.elmToolchain.elmFormatCLI ?: return

                        val editor = EditorFactory.getInstance().getEditors(document).first()

                        val result = elmFormat.formatDocumentAndSetText(project, document, elmVersion, addToUndoStack = false)

                        when (result) {
                            is ElmFormatResult.BadSyntax -> {
                                project.showBalloon(result.msg, NotificationType.WARNING, "Show Errors" to {
                                    val action = ActionManager.getInstance().getAction("Elm.Build")!!
                                    executeAction(action, "elm-format-notif", DataManager.getInstance().getDataContext(editor.component))
                                })
                            }

                            is ElmFormatResult.FailedToStart ->
                                project.showBalloon(result.msg, NotificationType.ERROR, "Configure" to { project.elmWorkspace.showConfigureToolchainUI() })

                            is ElmFormatResult.UnknownFailure ->
                                project.showBalloon(result.msg, NotificationType.ERROR)

                            is ElmFormatResult.Success ->
                                return
                        }
                    }
                }
        )
    }
}