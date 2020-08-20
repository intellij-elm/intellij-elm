package org.elm.workspace

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.elm.ide.notifications.showBalloon
import org.elm.openapiext.isSuccess
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.saveAllDocuments
import org.elm.utils.handleError
import org.elm.workspace.ElmToolchain.Companion.ELM_JSON
import java.nio.file.Files
import java.nio.file.Path


class ElmAttachProjectAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        val manifestName = ELM_JSON
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter { it.name == manifestName }
                .withTitle("Select '$manifestName' file")
                .apply { isForcedToUseIdeaFileChooser = true }

        val file = FileChooser.chooseFile(descriptor, project, null)
                ?: return

        if (file.name != manifestName) {
            Messages.showErrorDialog("Expected '$manifestName', got ${file.name}", "Invalid file type")
            return
        }

        val manifestPath = file.pathAsPath
        project.elmWorkspace.asyncAttachElmProject(manifestPath)
                .handleError { showError(it.cause!!, project, manifestPath) }
    }

    private fun showError(error: Throwable, project: Project, manifestPath: Path) {
        ApplicationManager.getApplication().invokeLater {
            // TODO [kl] double-check threading
            when (error) {
                is ProjectLoadException.MissingDependencies -> {
                    val answer = Messages.showYesNoDialog(
                            "Run 'elm make' to install packages?",
                            "Project dependencies may not be installed",
                            null
                    )
                    if (answer == Messages.YES) {
                        gracefullyRecover(project, error, manifestPath)
                    }
                }
                else -> Messages.showErrorDialog(error.message, "Failed to attach Elm project")
            }
        }
    }

    private fun gracefullyRecover(project: Project, error: ProjectLoadException.MissingDependencies, manifestPath: Path) {
        val elmCLI = project.elmToolchain.elmCLI
        if (elmCLI == null) {
            showBalloon(project, "Please set the path to the 'elm' binary", includeFixAction = true)
            return
        }

        // Install missing Elm package dependencies
        //
        // Unfortunately, there is no CLI command to install dependencies, so we instead
        // pick an arbitrary `.elm` file in the `source-directories` and tell Elm to
        // compile it. This will indirectly download and install the dependencies.

        val arbitraryElmFilePath = error.sourceDirectories.asSequence()
                .map { manifestPath.parent.resolve(it).normalize() }
                .flatMap { Files.newDirectoryStream(it).asSequence() }
                .firstOrNull()
                ?: return showBalloon(project, "Could not find a `.elm` file to compile")

        val output = elmCLI.make(project, workDir = manifestPath.parent, path = arbitraryElmFilePath)

        when {
            !output.isSuccess -> showBalloon(project, "Failed to compile $arbitraryElmFilePath")
            else -> {
                // Retry the original action. Hopefully this will work now!
                project.elmWorkspace.asyncAttachElmProject(manifestPath)
                        .handleError { showError(it.cause!!, project, manifestPath) }
            }
        }
    }

    private fun showBalloon(project: Project, message: String, includeFixAction: Boolean = false) {
        val actions = when {
            includeFixAction -> arrayOf("Fix" to { project.elmWorkspace.showConfigureToolchainUI() })
            else -> emptyArray()
        }
        project.showBalloon(message, NotificationType.ERROR, *actions)
    }
}