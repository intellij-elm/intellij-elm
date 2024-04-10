package org.elm.workspace.commandLineTools

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.ide.notifications.showBalloon
import org.elm.workspace.ElmProject
import org.elm.workspace.LamderaApplicationProject
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace
import java.nio.file.Path

fun makeProject(elmProject: ElmProject, project: Project, entryPoints: List<Triple<Path, String?, Int>?>, currentFileInEditor: VirtualFile?): Boolean {

    return if (elmProject is LamderaApplicationProject) {
        val lamderaCLI = project.elmToolchain.lamderaCLI
        if (lamderaCLI == null) {
            showError(project, "Please set the path to the 'lamdera' binary", includeFixAction = true)
            return false
        }
        lamderaCLI.make(project, elmProject.projectDirPath, elmProject, entryPoints, jsonReport = true, currentFileInEditor)
    } else {
        val elmCLI = project.elmToolchain.elmCLI
        if (elmCLI == null) {
            showError(project, "Please set the path to the 'elm' binary", includeFixAction = true)
            return false
        }
        elmCLI.make(project, elmProject.projectDirPath, elmProject, entryPoints, jsonReport = true, currentFileInEditor)
    }

}

private fun showError(project: Project, message: String, includeFixAction: Boolean = false) {
    val actions = if (includeFixAction)
        arrayOf("Fix" to { project.elmWorkspace.showConfigureToolchainUI() })
    else
        emptyArray()
    project.showBalloon(message, NotificationType.ERROR, *actions)
}
