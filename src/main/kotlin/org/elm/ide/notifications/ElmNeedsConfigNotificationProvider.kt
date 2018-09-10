package org.elm.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * Presents actionable notifications at the top of an Elm file whenever the Elm plugin
 * needs configuration (e.g. the path to the Elm compiler).
 */
class ElmNeedsConfigNotificationProvider(
        private val project: Project,
        private val notifications: EditorNotifications
) : EditorNotifications.Provider<EditorNotificationPanel>() {


    init {
        project.messageBus.connect(project).apply {
            subscribe(ElmWorkspaceService.WORKSPACE_TOPIC,
                    object : ElmWorkspaceService.ElmWorkspaceListener {
                        override fun didUpdate() {
                            notifications.updateAllNotifications()
                        }
                    })
        }
    }


    override fun getKey(): Key<EditorNotificationPanel> = PROVIDER_KEY


    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor)
            : EditorNotificationPanel? {
        if (!file.isElmFile || isNotificationDisabled())
            return null

        try {
            asyncAutoDiscoverWorkspace(project).get(1, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            // Auto-discover took too long. Do not show any errors: it may finish later with a good setup,
            // and if it doesn't we'll get another chance the next time a `.elm` file is opened.
            return null
        }

        val toolchain = project.elmToolchain
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel("No Elm toolchain configured")
        }

        val compilerVersion = toolchain.queryCompilerVersion()
        if (compilerVersion != null && compilerVersion < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION) {
            return createBadToolchainPanel("Elm $compilerVersion is not supported")
        }

        val workspace = project.elmWorkspace
        if (!workspace.hasAtLeastOneValidProject()) {
            return createNoElmProjectsPanel()
        }

        return null
    }


    private fun createBadToolchainPanel(message: String) =
            EditorNotificationPanel().apply {
                setText(message)
                createActionLabel("Setup toolchain") {
                    project.elmWorkspace.showConfigureToolchainUI()
                }
                createActionLabel("Do not show again") {
                    disableNotification()
                    notifications.updateAllNotifications()
                }
            }


    private fun createNoElmProjectsPanel() =
            EditorNotificationPanel().apply {
                setText("No Elm projects found")
                // TODO [drop 0.18] remove the `(or elm-package.json)` part
                createActionLabel("Attach elm.json (or elm-package.json)", "Elm.AttachElmProject")
                createActionLabel("Do not show again") {
                    disableNotification()
                    notifications.updateAllNotifications()
                }
            }


    private fun disableNotification() {
        PropertiesComponent.getInstance(project).setValue(NOTIFICATION_STATUS_KEY, true)
    }


    private fun isNotificationDisabled() =
            PropertiesComponent.getInstance(project).getBoolean(NOTIFICATION_STATUS_KEY)


    companion object {
        private val NOTIFICATION_STATUS_KEY = "org.elm.hideConfigNotifications"

        private val PROVIDER_KEY: Key<EditorNotificationPanel> = Key.create("Setup Elm toolchain")
    }

}
