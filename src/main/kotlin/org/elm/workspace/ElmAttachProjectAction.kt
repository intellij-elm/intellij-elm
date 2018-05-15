package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.ui.Messages
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.saveAllDocuments

class ElmAttachProjectAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()
        val filename= ElmToolchain.ELM_JSON
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter { it.name == filename }
                .withTitle("Select $filename")
        val chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val file = chooser.choose(project).singleOrNull()
                ?: return

        try {
            project.elmWorkspace.attachElmProject(file.pathAsPath)
        } catch (e: ProjectLoadException) {
            Messages.showErrorDialog(e.message, "Failed to attach Elm project")
        }
    }
}