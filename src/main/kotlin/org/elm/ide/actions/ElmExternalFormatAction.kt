package org.elm.ide.actions

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.isUnitTestMode
import org.elm.workspace.*


class ElmExternalFormatAction : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = getContext(e) != null
    }

    private fun getContext(e: AnActionEvent): Context? {
        val project = e.project ?: return null
        val toolchain = project.elmToolchain ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        if (!file.virtualFile.isInLocalFileSystem) return null
        if (file !is ElmFile) return null
        val elmVersion = when (val elmProject = file.elmProject) {
            is ElmApplicationProject -> elmProject.elmVersion
            is ElmPackageProject -> elmProject.elmVersion.low
            else -> return null
        }
        return Context(project, toolchain, file, elmVersion)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ctx = getContext(e) ?: return
        val project = ctx.project
        val virtualFile = ctx.elmFile.virtualFile
        val elmFormat = ctx.toolchain.elmFormat
        if (elmFormat == null) {
            project.showBalloon("could not find elm-format", NotificationType.ERROR)
            return
        }

        FileDocumentManager.getInstance().saveAllDocuments()
        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously<ProcessOutput, ExecutionException>({
                elmFormat.reformatFile(project, ctx.elmVersion, virtualFile)
            }, "Running elm-format on current file...", true, project)
            // We want to refresh file synchronously only in unit test
            // to get new text right after `reformatFile` call
            VfsUtil.markDirtyAndRefresh(!isUnitTestMode, true, true, virtualFile)
        } catch (e: ExecutionException) {
            if (isUnitTestMode) throw e
            val message = e.message ?: "something went wrong running elm-format"
            project.showBalloon(message, NotificationType.ERROR)
        }
    }

    private data class Context(
            val project: Project,
            val toolchain: ElmToolchain,
            val elmFile: ElmFile,
            val elmVersion: Version
    )
}

