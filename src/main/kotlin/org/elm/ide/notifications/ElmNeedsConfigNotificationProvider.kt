package org.elm.ide.notifications

import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.elm.lang.core.psi.isElmFile
import org.elm.openapiext.findFileByPath
import org.elm.workspace.*


/**
 * Presents actionable notifications at the top of an Elm file whenever the Elm plugin
 * needs configuration (e.g. the path to the Elm compiler).
 */
class ElmNeedsConfigNotificationProvider(
        private val project: Project,
        private val notifications: EditorNotifications
) : EditorNotifications.Provider<EditorNotificationPanel>() {

    private val workspaceChangedTracker = SimpleModificationTracker()

    init {
        project.messageBus.connect(project).apply {
            subscribe(ElmWorkspaceService.WORKSPACE_TOPIC,
                    object : ElmWorkspaceService.ElmWorkspaceListener {
                        override fun didUpdate() {
                            workspaceChangedTracker.incModificationCount()
                            notifications.updateAllNotifications()
                        }
                    })
        }
    }


    override fun getKey(): Key<EditorNotificationPanel> = PROVIDER_KEY


    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!file.isElmFile)
            return null

        val toolchain = project.elmToolchain
        if (!toolchain.looksLikeValidToolchain()) {
            return badToolchainPanel("You must specify a path to the Elm compiler")
        }

        val workspace = project.elmWorkspace
        if (!workspace.hasAtLeastOneValidProject()) {
            return noElmProjectPanel("No Elm projects found")
        }

        val elmProject = project.elmWorkspace.findProjectForFile(file)
                ?: return noElmProjectPanel("Could not find Elm project for this file")

        val compilerVersion = toolchain.elmCLI?.queryVersion()?.orNull()
                ?: return badToolchainPanel("Could not determine Elm compiler version")

        when {
            elmProject is ElmApplicationProject && !elmProject.elmVersion.looseEquals(compilerVersion) ->
                return versionConflictPanel(project, elmProject, elmProject.elmVersion.toString(), compilerVersion)
            elmProject is ElmPackageProject && !elmProject.elmVersion.contains(compilerVersion) ->
                return versionConflictPanel(project, elmProject, elmProject.elmVersion.toString(), compilerVersion)
        }

        return null
    }


    private fun badToolchainPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
                createActionLabel("Setup toolchain") {
                    project.elmWorkspace.showConfigureToolchainUI()
                }
            }


    private fun noElmProjectPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
                // TODO [drop 0.18] remove the `(or elm-package.json)` part
                createActionLabel("Attach elm.json (or elm-package.json)", "Elm.AttachElmProject")
            }


    private fun versionConflictPanel(project: Project, elmProject: ElmProject, expectedVersionText: String, compilerVersion: Version): EditorNotificationPanel {
        val manifestFileName = elmProject.manifestPath.fileName.toString()
        return EditorNotificationPanel().apply {
            setText("Your $manifestFileName file requires Elm $expectedVersionText but your Elm compiler is $compilerVersion")
            createActionLabel("Open $manifestFileName") {
                val didNavigate = LocalFileSystem.getInstance().findFileByPath(elmProject.manifestPath)
                        ?.let { OpenFileDescriptor(project, it) }
                        ?.navigateInEditor(project, true)
                if (didNavigate != true)
                    project.showBalloon("Cannot open $manifestFileName", NotificationType.ERROR)
            }
            createActionLabel("Setup toolchain") {
                project.elmWorkspace.showConfigureToolchainUI()
            }
        }
    }


    companion object {
        private val PROVIDER_KEY: Key<EditorNotificationPanel> = Key.create("Setup Elm toolchain")
    }

}
