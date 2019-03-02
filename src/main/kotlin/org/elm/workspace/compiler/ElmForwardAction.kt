package org.elm.workspace.compiler

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.messages.Topic
import org.elm.ide.toolwindow.ElmCompilerPanel

class ElmForwardAction(private val elmCompilerPanel: ElmCompilerPanel) : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Next Error"
        e.presentation.icon = AllIcons.Actions.Forward
        e.presentation.isEnabled = !elmCompilerPanel.compilerMessages.isEmpty() && elmCompilerPanel.canForward
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        project.messageBus.syncPublisher(ERRORS_FORWARD_TOPIC).forward()
    }

    interface ElmErrorsForwardListener {
        fun forward()
    }

    companion object {
        val ERRORS_FORWARD_TOPIC = Topic("Elm Compiler Errors Forward", ElmErrorsForwardListener::class.java)
    }
}
