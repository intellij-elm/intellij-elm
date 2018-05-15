package org.elm.workspace

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ElmDetachProjectAction: AnAction() {

    // TODO [kl] let the user choose the project to detach in the Elm project tool window

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return

        val manifestPath = null // TODO [kl] change me
        project.elmWorkspace.detachElmProject(manifestPath)
    }
}