package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.saveAllDocuments
import org.elm.utils.handleError

private val ACCEPTABLE_FILE_NAMES = listOf(ElmToolchain.ELM_JSON, ElmToolchain.ELM_LEGACY_JSON)

class ElmAttachProjectAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter { it.name in ACCEPTABLE_FILE_NAMES }
                .withTitle("Select elm.json (Elm 0.19) or elm-package.json (Elm 0.18)") // TODO [drop 0.18]
        descriptor.isForcedToUseIdeaFileChooser = true

        val file = FileChooser.chooseFile(descriptor, project, null)
                ?: return

        if (file.name !in ACCEPTABLE_FILE_NAMES) {
            // TODO [drop 0.18]
            Messages.showErrorDialog("Expected elm.json or elm-package.json, got ${file.name}", "Invalid file type")
            return
        }

        project.elmWorkspace.asyncAttachElmProject(file.pathAsPath)
                .handleError { showError(it) }
    }

    private fun showError(error: Throwable) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(error.message, "Failed to attach Elm project")
        }
    }
}