package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.elm.openapiext.saveAllDocuments

class ElmRefreshProjectsAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        if (project.elmToolchain == null || !project.hasAnElmProject) {
            guessAndSetupElmProject(project, explicitRequest = true)
        } else {
            project.elmWorkspace.refreshAllProjects()
        }
    }
}