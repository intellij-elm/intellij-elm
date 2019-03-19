package org.elm.ide.actions

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.isElmFile
import org.elm.openapiext.isUnitTestMode
import org.elm.workspace.Version
import org.elm.workspace.commandLineTools.ElmFormatCLI
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

        val fixAction = "Fix" to { ctx.project.elmWorkspace.showConfigureToolchainUI() }

        val elmFormat = ctx.project.elmToolchain.elmFormatCLI
        if (elmFormat == null) {
            ctx.project.showBalloon("Could not find elm-format", NotificationType.ERROR, fixAction)
            return
        }

        try {
            elmFormat.formatDocumentAndSetText(ctx.project, ctx.document, ctx.elmVersion, addToUndoStack = true)
        } catch (ex: ExecutionException) {
            if (isUnitTestMode) throw ex
            val message = ex.message ?: "something went wrong running elm-format"
            if (message.contains("SYNTAX PROBLEM", ignoreCase = true)) {
                ctx.project.showBalloon("Please fix the syntax errors before running elm-format.", NotificationType.WARNING)
            } else {
                ctx.project.showBalloon(message, NotificationType.ERROR, fixAction)
            }
        }
    }

    private fun getContext(e: AnActionEvent): Context? {
        val project = e.project ?: return null
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        if (!file.isInLocalFileSystem) return null
        if (!file.isElmFile) return null
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val elmVersion = ElmFormatCLI.getElmVersion(project, file) ?: return null
        return Context(project, file, document, elmVersion)
    }

    data class Context(
            val project: Project,
            val file: VirtualFile,
            val document: Document,
            val elmVersion: Version
    )

    companion object {
        const val ID = "Elm.RunExternalElmFormat" // must stay in-sync with `plugin.xml`
    }
}
