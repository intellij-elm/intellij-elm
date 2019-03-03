package org.elm.workspace.compiler

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.messages.Topic

class ElmBackAction : AnAction() {

    var enabled: Boolean = false

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Previous Error"
        e.presentation.icon = AllIcons.Actions.Back
        e.presentation.isEnabled = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        project.messageBus.syncPublisher(ERRORS_BACK_TOPIC).back()
    }

    interface ElmErrorsBackListener {
        fun back()
    }

    companion object {
        val ERRORS_BACK_TOPIC = Topic("Elm Compiler Errors Back", ElmErrorsBackListener::class.java)
    }
}
