package org.elm.ide.actions

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.ElmFileType
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.*
import org.elm.workspace.commandLineTools.makeProject
import org.elm.workspace.compiler.findEntrypoints

private val log = logger<ElmExternalReviewAction>()

class ElmExternalReviewAction : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.project != null
    }

    private fun showError(project: Project, message: String, includeFixAction: Boolean = false) {
        val actions = if (includeFixAction)
            arrayOf("Fix" to { project.elmWorkspace.showConfigureToolchainUI() })
        else
            emptyArray()
        project.showBalloon(message, NotificationType.ERROR, *actions)
    }

    private fun findActiveFile(e: AnActionEvent, project: Project): VirtualFile? =
        e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileEditorManager.getInstance(project).selectedFiles.firstOrNull { it.fileType == ElmFileType }

    override fun actionPerformed(e: AnActionEvent) {
        saveAllDocuments()
        val project = e.project ?: return

        val activeFile = findActiveFile(e, project)
            ?: return showError(project, "Could not determine active Elm file")

        if (activeFile.canonicalPath != null) {
            // TODO improve exclusion of elm-review project
            if (activeFile.canonicalPath!!.contains("/review")) return
        }

        val elmReviewCLI = project.elmToolchain.elmReviewCLI
            ?: return showError(project, "Please set the path to the 'elm-review' binary", includeFixAction = true)

        val fixAction = "Fix" to { project.elmWorkspace.showConfigureToolchainUI() }

        val elmReview = project.elmToolchain.elmReviewCLI
        if (elmReview == null) {
            project.showBalloon("Could not find elm-review", NotificationType.ERROR, fixAction)
            return
        }

        val elmProject = project.elmWorkspace.findProjectForFile(activeFile)
            ?: return showError(project, "Could not determine active Elm project")

        val projectDir = VfsUtil.findFile(elmProject.projectDirPath, true)
            ?: return showError(project, "Could not determine active Elm project's path")

        val entryPoints = // list of (filePathToCompile, targetPath, offset)
            findEntrypoints(elmProject, project, projectDir, activeFile)

        try {
            val currentFileInEditor: VirtualFile? = e.getData(PlatformDataKeys.VIRTUAL_FILE)
            val compiledSuccessfully = makeProject(elmProject, project, entryPoints, currentFileInEditor)
            if (compiledSuccessfully) {
                elmReviewCLI.runReview(project, elmProject, project.elmToolchain.elmCLI, currentFileInEditor)
            }
        } catch (e: ExecutionException) {
            return showError(
                project,
                "Failed to 'make' or 'review'. Are the path settings correct ?",
                includeFixAction = true
            )
        }
    }
}
