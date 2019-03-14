package org.elm.workspace.compiler

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.messages.Topic
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.findInference
import org.elm.openapiext.isUnitTestMode
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.*


class ElmBuildAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        saveAllDocuments()
        val project = e.project ?: return

        val elmCLI = project.elmToolchain.elmCLI
                ?: return showError(project, "Please set the path to the 'elm' binary", includeFixAction = true)

        val elmFile = findActiveFile(e, project)
                ?: return showError(project, "Could not determine active Elm file")

        val elmProject = project.elmWorkspace.findProjectForFile(elmFile)
                ?: return showError(project, "Could not determine active Elm project")

        if (elmProject.isElm18)
            return showError(project, "The Elm 0.18 compiler is not supported.")

        val filePathToCompile = when (elmProject) {
            is ElmApplicationProject -> {
                findMainEntryPoint(project, elmProject)?.containingFile?.virtualFile?.pathAsPath
                        ?: return showError(project, "Cannot find your Elm app's main entry point. Please make sure that it has a type annotation.")
            }

            is ElmPackageProject ->
                elmFile.pathAsPath
        }

        val manifestBaseDir = VfsUtil.findFile(elmProject.manifestPath.parent, /*refresh*/ true)
                ?: return showError(project, "Could not find Elm-Project base directory")

        val json = try {
            elmCLI.make(project, elmProject, filePathToCompile).stderr
        } catch (e: ExecutionException) {
            return showError(project, "Failed to run the 'elm' executable. Is the path correct?", includeFixAction = true)
        }

        val messages = if (json.isEmpty()) emptyList() else {
            elmJsonToCompilerMessages(json).sortedWith(
                    compareBy(
                            { it.title },
                            { it.location?.region?.start?.line },
                            { it.location?.region?.start?.column }
                    ))
        }
        project.messageBus.syncPublisher(ERRORS_TOPIC).update(manifestBaseDir, messages)
        if (isUnitTestMode) return
        ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler").show(null)
    }

    private fun findMainEntryPoint(project: Project, elmProject: ElmProject): ElmFunctionDeclarationLeft? =
            ElmLookup.findByName<ElmFunctionDeclarationLeft>("main", LookupClientLocation(project, elmProject))
                    .find { decl ->
                        val ty = decl.findInference()?.ty
                        val key = when (ty) {
                            is TyUnion -> ty.module to ty.name
                            is TyUnknown -> ty.alias?.let { it.module to it.name }
                            else -> null
                        }
                        key != null && key in elmMainTypes
                    }

    private fun findActiveFile(e: AnActionEvent, project: Project): VirtualFile? =
            e.getData(CommonDataKeys.VIRTUAL_FILE)
                    ?: FileEditorManager.getInstance(project).selectedFiles.firstOrNull { it.fileType == ElmFileType }

    private fun showError(project: Project, message: String, includeFixAction: Boolean = false) {
        val actions = if (includeFixAction)
            arrayOf("Fix" to { project.elmWorkspace.showConfigureToolchainUI() })
        else
            emptyArray()
        project.showBalloon(message, NotificationType.ERROR, *actions)
    }

    interface ElmErrorsListener {
        fun update(baseDir: VirtualFile, messages: List<ElmError>)
    }

    companion object {
        val ERRORS_TOPIC = Topic("Elm compiler-messages", ElmErrorsListener::class.java)
        private val elmMainTypes = setOf("Platform" to "Program", "Html" to "Html")
    }

    data class LookupClientLocation(
            override val intellijProject: Project,
            override val elmProject: ElmProject?,
            override val isInTestsDirectory: Boolean = false
    ) : ClientLocation
}
