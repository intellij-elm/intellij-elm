package org.elm.workspace.compiler

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.messages.Topic
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.findInference
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.ElmProject
import org.elm.workspace.ElmWorkspaceService
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace


class ElmBuildAction : AnAction() {

    private val elmJsonReport = ElmJsonReport()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        val elmCLI = project.elmToolchain.elmCLI
        val fixAction = "Fix" to { project.elmWorkspace.showConfigureToolchainUI() }
        if (elmCLI == null) {
            project.showBalloon("Could not find elm", NotificationType.ERROR, fixAction)
            return
        }

        val elmProject = findElmProject(e)
        if (elmProject == null) {
            project.showBalloon("Could not Elm project", NotificationType.ERROR)
            return
        }

        // find "main" function
        val mainFuncDecl = ElmLookup
                .findByName<ElmFunctionDeclarationLeft>("main", LookupClientLocation(project, elmProject))
                .find {
                    val ty = (it.findInference()?.ty as? TyUnion) ?: return@find false
                    elmMainTypes.contains(ty.module to ty.name)
                }

        if (mainFuncDecl == null) {
            showDialog(project, "Cannot find your Elm app's main entry point. Please make sure that it has a type annotation.")
            return
        }

        val manifestBaseDir = elmProject.let { VfsUtil.findFile(elmProject.manifestPath.parent, /*refresh*/ true) }
        if (manifestBaseDir == null) {
            showDialog(project, "Could not find Elm-Project base directory")
            return
        }

        val json = try {
            elmCLI.make(project, elmProject, mainFuncDecl.containingFile.virtualFile.path).stderr
        } catch (e: ExecutionException) {
            project.showBalloon("Invalid path for 'elm' executable. Only Elm >= 0.19 is supported in for Build action.", NotificationType.ERROR, fixAction)
            return
        }

        val messages = if (json.isEmpty()) emptyList() else {
            elmJsonReport.elmToCompilerMessages(json).sortedWith(
                    compareBy(
                            { it.name },
                            { it.messageWithRegion.region.start.line },
                            { it.messageWithRegion.region.start.column }
                    ))
        }
        project.messageBus.syncPublisher(ERRORS_TOPIC).update(manifestBaseDir, messages)

        // show toolwindow
        ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler").show(null)
    }

    private fun findElmProject(e: AnActionEvent): ElmProject? {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        e.project?.let {
            val elmWorkspaceService = ServiceManager.getService(it, ElmWorkspaceService::class.java)
            return elmWorkspaceService.findProjectForFile(file)
        }
        return null
    }

    private fun showDialog(project: Project, message: String) {
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null).setFadeoutTime(5000).createBalloon().show(RelativePoint.getNorthEastOf(statusBar.component), Balloon.Position.atRight)
    }

    interface ElmErrorsListener {
        fun update(baseDir: VirtualFile, messages: List<CompilerMessage>)
    }

    companion object {
        val ERRORS_TOPIC = Topic("Elm compiler-messages", ElmErrorsListener::class.java)
        private val elmMainTypes: Set<Pair<String, String>> = hashSetOf(Pair("Platform", "Program"), Pair("Html", "Html"))
    }

    data class LookupClientLocation(
            override val intellijProject: Project,
            override val elmProject: ElmProject?,
            override val isInTestsDirectory: Boolean = false
    ) : ClientLocation
}
