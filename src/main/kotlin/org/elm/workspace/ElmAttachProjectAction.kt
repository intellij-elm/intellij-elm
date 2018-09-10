package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.ui.Messages
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.saveAllDocuments

class ElmAttachProjectAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()
        val filename = ElmToolchain.ELM_JSON
        val legacyFilename = ElmToolchain.ELM_LEGACY_JSON // TODO [drop 0.18] remove this legacy stuff
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter { it.name == filename || it.name == legacyFilename }
                .withTitle("Select $filename (Elm 0.19) or $legacyFilename (Elm 0.18)")
        val chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val file = chooser.choose(project).singleOrNull()
                ?: return

        try {
            project.elmWorkspace.asyncAttachElmProject(file.pathAsPath)
        } catch (e: ProjectLoadException) {
            Messages.showErrorDialog(e.message, "Failed to attach Elm project")
        }
    }
}