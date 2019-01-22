package org.elm.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.elm.lang.core.psi.isElmFile
import org.elm.workspace.*

private val versionCacheKey: Key<CachedValue<Version>> = Key.create("ELM_VERSION_CACHE")

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
        if (!file.isElmFile || isNotificationDisabled())
            return null

        val elmProject = project.elmWorkspace.findProjectForFile(file)

        if (false) {
            // simple debugging for issues with associating Elm files with Elm projects
            return EditorNotificationPanel().apply {
                setText(elmProject?.projectDirPath?.toString() ?: "No Elm project associated with this file")
            }
        }

        val toolchain = project.elmToolchain
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel("No Elm toolchain configured")
        }

        val elmVersion = CachedValuesManager.getManager(project)
                .getCachedValue(project, versionCacheKey, {
                    val v = toolchain.queryCompilerVersion() ?: Version.UNKNOWN
                    CachedValueProvider.Result.create(v, workspaceChangedTracker)
                }, false)

        if (elmVersion < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION) {
            return createBadToolchainPanel("Elm $elmVersion is not supported")
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
