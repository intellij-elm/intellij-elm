package org.elm.workspace.compiler

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.elm.ide.notifications.showBalloon
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.types.TyUnion
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.findTy
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.pathRelative
import org.elm.openapiext.saveAllDocuments
import org.elm.workspace.*
import java.nio.file.Path

val ELM_BUILD_ACTION_ID = "Elm.Build"

class ElmBuildAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        saveAllDocuments()
        val project = e.project ?: return

        val elmCLI = project.elmToolchain.elmCLI
            ?: return showError(project, "Please set the path to the 'elm' binary", includeFixAction = true)

        val activeFile = findActiveFile(e, project)
            ?: return showError(project, "Could not determine active Elm file")

        if (ElmFile.fromVirtualFile(activeFile, project)?.isInTestsDirectory == true)
            return showError(project, "To check tests for compile errors, use the elm-test run configuration instead.")

        val elmProject = project.elmWorkspace.findProjectForFile(activeFile)
            ?: return showError(project, "Could not determine active Elm project")

        val projectDir = VfsUtil.findFile(elmProject.projectDirPath, true)
            ?: return showError(project, "Could not determine active Elm project's path")

        val entryPoints: List<Triple<Path, String?, Int>?> = // (filePathToCompile, targetPath, offset)
            when (elmProject) {
                is LamderaApplicationProject -> {
                    val mainEntryPoints = findAppEntryPoints(project, elmProject)
                    val result = mainEntryPoints.map { mainEntryPoint ->
                        mainEntryPoint.containingFile.virtualFile?.let {
                            Triple(
                                it.pathRelative(project),
                                VfsUtilCore.getRelativePath(it, projectDir),
                                mainEntryPoint.textOffset
                            )
                        }
                    }
                    result.ifEmpty {
                        return showError(
                            project,
                            "Cannot find your Lamdera app's Frontend & Backend entry points. Please make sure that it named 'app' in both Frontend- and Backend-module."
                        )
                    }
                }

                is ElmApplicationProject -> {
                    val mainEntryPoints = findMainEntryPoint(project, elmProject)
                    val result = mainEntryPoints.map { mainEntryPoint ->
                        mainEntryPoint.containingFile.virtualFile?.let {
                            Triple(
                                it.pathRelative(project),
                                VfsUtilCore.getRelativePath(it, projectDir),
                                mainEntryPoint.textOffset
                            )
                        }
                    }
                    result.ifEmpty {
                        return showError(
                            project,
                            "Cannot find your Elm app's main entry point. Please make sure that it has a type annotation."
                        )
                    }
                }

                is ElmPackageProject ->
                    listOf(
                        Triple(
                            activeFile.pathAsPath,
                            VfsUtilCore.getRelativePath(activeFile, projectDir),
                            0
                        )
                    )
            }

        try {
            elmCLI.make(project, elmProject.projectDirPath, elmProject, entryPoints, jsonReport = true)
        } catch (e: ExecutionException) {
            return showError(
                project,
                "Failed to run the '${elmCLI.elmExecutablePath}' executable. Is the path correct?",
                includeFixAction = true
            )
        }
    }

    private fun findAppEntryPoints(project: Project, elmProject: ElmProject): List<ElmFunctionDeclarationLeft> {
        val clientLocation = object : ClientLocation {
            override val intellijProject: Project
                get() = project
            override val elmProject: ElmProject
                get() = elmProject
            override val isInTestsDirectory: Boolean
                get() = false
        }
        val lamderaBackend = ElmLookup.findByNameAndModule<ElmFunctionDeclarationLeft>("app", "Backend", clientLocation)
        val lamderaFrontend = ElmLookup.findByNameAndModule<ElmFunctionDeclarationLeft>("app", "Frontend", clientLocation)
        return lamderaFrontend + lamderaBackend
    }

    private fun findMainEntryPoint(project: Project, elmProject: ElmProject): List<ElmFunctionDeclarationLeft> {
        val elmEntry =
            ElmLookup.findByName<ElmFunctionDeclarationLeft>("main", LookupClientLocation(project, elmProject))
                .find { decl ->
                    val key = when (val ty = decl.findTy()) {
                        is TyUnion -> ty.module to ty.name
                        is TyUnknown -> ty.alias?.let { it.module to it.name }
                        else -> null
                    }
                    key != null && key in elmMainTypes && decl.isTopLevel
                }
        return elmEntry?.let { listOf(it) } ?: emptyList()
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
        fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String, offset: Int)
    }

    companion object {
        val ERRORS_TOPIC = Topic("Elm compiler-messages", ElmErrorsListener::class.java)
        private val lamderaAppTypes = setOf(
            "Frontend" to "app",
            "Backend" to "app"
        )
        private val elmMainTypes = setOf(
            "Platform" to "Program",
            "Html" to "Html",
            "VirtualDom" to "Node"
        )
    }

    data class LookupClientLocation(
        override val intellijProject: Project,
        override val elmProject: ElmProject?,
        override val isInTestsDirectory: Boolean = false
    ) : ClientLocation
}
