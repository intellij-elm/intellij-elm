package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import org.elm.openapiext.saveAllDocuments
import org.elm.utils.handleError

class ElmRefreshProjectsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        if (!(project.elmToolchain.looksLikeValidToolchain() && project.elmWorkspace.hasAtLeastOneValidProject())) {
            asyncAutoDiscoverWorkspace(project, explicitRequest = true)
        } else {
            project.elmWorkspace.asyncRefreshAllProjects(installDeps = true)
        }.handleError {
            showError(it)
        }
    }

    private fun showError(error: Throwable) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(error.message, "Failed to refresh Elm projects")
        }
    }
}