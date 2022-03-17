package org.elm.ide.actions

import com.intellij.ide.DataManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.ide.notifications.executeAction
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.isElmFile
import org.elm.openapiext.isUnitTestMode
import org.elm.workspace.Version
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI.ElmFormatResult
import org.elm.workspace.compiler.ELM_BUILD_ACTION_ID
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace


class ElmExternalFormatAction : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = getContext(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ctx = getContext(e)
        if (ctx == null) {
            if (isUnitTestMode) error("should not happen: context is null!")
            return
        }

        val project = ctx.project
        val editor = ctx.editor
        val document = editor.document

        val configureFixAction = "Configure" to { project.elmWorkspace.showConfigureToolchainUI() }

        val elmFormat = project.elmToolchain.elmFormatCLI
        if (elmFormat == null) {
            project.showBalloon("Could not find elm-format", NotificationType.ERROR, configureFixAction)
            return
        }

        val result = elmFormat.formatDocumentAndSetText(project, document, ctx.elmVersion, addToUndoStack = true)
        when (result) {
            is ElmFormatResult.BadSyntax -> {
                project.showBalloon(result.msg, NotificationType.WARNING, "Show Errors" to {
                    val action = ActionManager.getInstance().getAction(ELM_BUILD_ACTION_ID)!!
                    executeAction(action, "elm-format-notif", DataManager.getInstance().getDataContext(editor.component))
                })
            }

            is ElmFormatResult.FailedToStart ->
                project.showBalloon(result.msg, NotificationType.ERROR, configureFixAction)

            is ElmFormatResult.UnknownFailure ->
                project.showBalloon(result.msg, NotificationType.ERROR)

            is ElmFormatResult.Success ->
                return
        }
    }

    private fun getContext(e: AnActionEvent): Context? {
        val project = e.project ?: return null
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        if (!file.isInLocalFileSystem) return null
        if (!file.isElmFile) return null
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return null
        val elmVersion = ElmFormatCLI.getElmVersion(project, file) ?: return null
        return Context(project, file, editor, elmVersion)
    }

    data class Context(
            val project: Project,
            val file: VirtualFile,
            val editor: Editor,
            val elmVersion: Version
    )

    companion object {
        const val ID = "Elm.RunExternalElmFormat" // must stay in-sync with `plugin.xml`
    }
}
