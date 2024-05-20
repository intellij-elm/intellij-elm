package org.elm.workspace

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey


class ElmDetachProjectAction : AnAction() {


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.associatedElmProject != null
    }


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val elmProject = e.associatedElmProject ?: return
        project.elmWorkspace.detachElmProject(elmProject.manifestPath)
    }

    // Possibly EDT is a better choice here than BGT; though BGT seemed to be the safest bet.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT


    private val AnActionEvent.associatedElmProject: ElmProject?
        get() = dataContext.getData(DATA_KEY)


    companion object {
        val DATA_KEY = DataKey.create<ElmProject>("ELM_PROJECT_TO_DETACH")
    }
}