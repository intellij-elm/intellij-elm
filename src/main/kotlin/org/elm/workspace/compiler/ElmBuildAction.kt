package org.elm.workspace.compiler

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.findInference
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.ElmProject
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace

class ElmBuildAction : AnAction() {

    private val elmMainTypes: Set<Pair<String, String>> = hashSetOf(Pair("Platform", "Program"), Pair("Html", "Html"))

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

        // find "main" function
        val clientLocation = LookupClientLocation(project, elmProject)
        val elements = ElmLookup.findByName<ElmNamedElement>("main", clientLocation)
        val mainElements = elements
                .filter { element -> element.findInference() != null && element.findInference()?.ty is TyUnion }
                .filter { element -> elmMainTypes.contains(Pair((element.findInference()?.ty as TyUnion).module, (element.findInference()?.ty as TyUnion).name)) }

        if (mainElements.isNotEmpty()) {
            val path = mainElements[0].containingFile.virtualFile.path
            val json = elmCLI.make(project, elmProject, path).stderr

            if (json.isNotEmpty()) {

                // TODO list of errors only, if multiple independent modules (with errors) are compiled

                val messages = elmJsonReport.elmToCompilerMessages(json)
                project.messageBus.syncPublisher(ERRORS_TOPIC).update(messages)

            } else {
                project.messageBus.syncPublisher(ERRORS_TOPIC).update(emptyList())
            }
        } else {
            showDialog(project)
        }

    }

    private fun showDialog(project: Project) {
        Messages.showDialog(project, "No Type Signature found. Please specify one, so that Elm Build works.", "Info", arrayOf("Ok"), 0, Messages.getErrorIcon())
    }

    interface ElmErrorsListener {
        fun update(messages: List<CompilerMessage>)
    }

    companion object {
        val ERRORS_TOPIC = Topic("Elm compiler-messages", ElmErrorsListener::class.java)
    }

    data class LookupClientLocation(
            override val intellijProject: Project,
            override val elmProject: ElmProject?,
            override val isInTestsDirectory: Boolean = false
    ) : ClientLocation
}
