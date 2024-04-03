package org.elm.ide.listeners

import com.intellij.ide.DataManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI.ElmFormatResult
import org.elm.workspace.elmSettings
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace


class ElmFormatOnFileSaveListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val dataContext = DataManager.getInstance().dataContext
        CommonDataKeys.PROJECT.getData(dataContext)?.let { project ->
            if (!project.elmSettings.toolchain.isElmFormatOnSaveEnabled) return
            val vFile = FileDocumentManager.getInstance().getFile(document) ?: return
            if (!vFile.isElmFile) return
            val elmVersion = ElmFormatCLI.getElmVersion(project, vFile) ?: return
            val elmFormat = project.elmToolchain.elmFormatCLI ?: return

            val result = elmFormat.formatDocumentAndSetText(project, document, elmVersion, addToUndoStack = false)
            when (result) {
                is ElmFormatResult.BadSyntax ->
                    project.showBalloon(result.msg, NotificationType.WARNING)

                is ElmFormatResult.FailedToStart ->
                    project.showBalloon(
                        result.msg,
                        NotificationType.ERROR,
                        "Configure" to { project.elmWorkspace.showConfigureToolchainUI() }
                    )

                is ElmFormatResult.UnknownFailure ->
                    project.showBalloon(result.msg, NotificationType.ERROR)

                is ElmFormatResult.Success ->
                    return
            }
        }
    }
}
