package org.elm.workspace.compiler

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace

class ElmBuildAction : AnAction() {

    private val elmJsonReport = ElmJsonReport()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        val elmCLI = project.elmToolchain.elmCLI
        if (elmCLI == null) {
            Messages.showErrorDialog("No path to the Elm compiler", "Build Error")
            return
        }

        val elmProject = project.elmWorkspace.allProjects.first()
        val json = elmCLI.make(project, elmProject).stderr

        if (json.isNotEmpty()) {

            // TODO list of errors only, if _independent_ modules (with errors) are compiled

            val messages = elmJsonReport.elmToCompilerMessages(json)
            project.messageBus.syncPublisher(ERRORS_TOPIC).update(messages)

        } else {
            project.messageBus.syncPublisher(ERRORS_TOPIC).update(emptyList())
        }
    }

    interface ElmErrorsListener {
        fun update(messages: List<CompilerMessage>)
    }

    companion object {
        val ERRORS_TOPIC = Topic("Elm compiler-messages", ElmErrorsListener::class.java)
    }
}
