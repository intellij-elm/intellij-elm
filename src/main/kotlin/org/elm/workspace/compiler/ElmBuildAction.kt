package org.elm.workspace.compiler

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.messages.Topic
import org.elm.ide.notifications.showBalloon
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
            val fixAction = "Fix" to { project.elmWorkspace.showConfigureToolchainUI() }
            project.showBalloon("Could not find elm", NotificationType.ERROR, fixAction)
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

                // TODO test _list_ of errors (only produced, if multiple independent erroneous modules are compiled)

                val messages = elmJsonReport.elmToCompilerMessages(json).sortedWith(compareBy({it.name}, {it.messageWithRegion.region.start.line}, {it.messageWithRegion.region.start.column}))
                project.messageBus.syncPublisher(ERRORS_TOPIC).update(messages)

            } else {
                project.messageBus.syncPublisher(ERRORS_TOPIC).update(emptyList())
            }
            // show toolwindow
            ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler").show(null)
        } else {
            showDialog(project)
        }

    }

    private fun showDialog(project: Project) {
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("No Type Signature found. Please specify one, so that Elm Build works.", MessageType.ERROR, null).setFadeoutTime(5000).createBalloon().show(RelativePoint.getNorthEastOf(statusBar.component), Balloon.Position.atRight)
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
