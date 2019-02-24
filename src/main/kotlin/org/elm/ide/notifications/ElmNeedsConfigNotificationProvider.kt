package org.elm.ide.notifications

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
import org.elm.openapiext.Result
import org.elm.workspace.*

private val versionCacheKey: Key<CachedValue<Result<Version>>> = Key.create("ELM_VERSION_CACHE")

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
            return createBadToolchainPanel("Elm toolchain needs configuration")
        }

        val elmCLI = toolchain.elmCLI
                ?: return createBadToolchainPanel("Need path to Elm compiler")

        val elmVersionResult = CachedValuesManager.getManager(project)
                .getCachedValue(project, versionCacheKey, {
                    val v = elmCLI.queryVersion()
                    CachedValueProvider.Result.create(v, workspaceChangedTracker)
                }, false)

        when (elmVersionResult) {
            is Result.Ok -> {
                if (elmVersionResult.value < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION) {
                    return createBadToolchainPanel("Elm ${elmVersionResult.value} is not supported")
                }
            }
            is Result.Err ->
                return createBadToolchainPanel("Invalid Elm toolchain: ${elmVersionResult.reason}")
        }

        val workspace = project.elmWorkspace
        if (!workspace.hasAtLeastOneValidProject()) {
            return createNoElmProjectPanel("No Elm projects found")
        }

        if (project.elmWorkspace.findProjectForFile(file) == null) {
            return createNoElmProjectPanel("Could not find Elm project for this file")
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


    companion object {
        private val PROVIDER_KEY: Key<EditorNotificationPanel> = Key.create("Setup Elm toolchain")
    }

}
