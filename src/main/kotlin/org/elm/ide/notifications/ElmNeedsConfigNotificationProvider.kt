package org.elm.ide.notifications

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.ElmApplicationProject
import org.elm.workspace.ElmWorkspaceService
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace


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
            return createBadToolchainPanel("You must specify a path to the Elm compiler")
        }

        val workspace = project.elmWorkspace
        if (!workspace.hasAtLeastOneValidProject()) {
            return createNoElmProjectPanel("No Elm projects found")
        }

        val elmProject = project.elmWorkspace.findProjectForFile(file)
                ?: return createNoElmProjectPanel("Could not find Elm project for this file")

        val elmExecutableVersion = toolchain.elmCLI?.queryVersion()?.orNull()
                ?: return createBadToolchainPanel("Could not determine Elm compiler version")

        if (elmProject is ElmApplicationProject && elmProject.elmVersion != elmExecutableVersion) {
            return createGenericPanel("Your elm.json file requires Elm ${elmProject.elmVersion} but your Elm compiler is $elmExecutableVersion")
        }

        return null
    }


    private fun createBadToolchainPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
                createActionLabel("Setup toolchain") {
                    project.elmWorkspace.showConfigureToolchainUI()
                }
            }


    private fun createNoElmProjectPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
                // TODO [drop 0.18] remove the `(or elm-package.json)` part
                createActionLabel("Attach elm.json (or elm-package.json)", "Elm.AttachElmProject")
            }

    private fun createGenericPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
            }


    companion object {
        private val PROVIDER_KEY: Key<EditorNotificationPanel> = Key.create("Setup Elm toolchain")
    }

}
