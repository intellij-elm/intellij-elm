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
import org.elm.workspace.ElmToolchain.Companion.ELM_JSON


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

        project.elmWorkspace.asyncAttachElmProject(file.pathAsPath)
                .handleError { showError(it) }
    }

    private fun showError(error: Throwable) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(error.message, "Failed to attach Elm project")
        }
    }
}