/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.workspace

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.Consumer
import com.intellij.util.indexing.LightDirectoryIndex
import com.intellij.util.io.exists
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.messages.Topic
import org.elm.ide.notifications.showBalloon
import org.elm.openapiext.findFileByPath
import org.elm.openapiext.modules
import org.elm.openapiext.pathAsPath
import org.elm.workspace.ui.ElmWorkspaceConfigurable
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference


private val log = logger<ElmWorkspaceService>()


/**
 * The workspace can hold multiple [ElmProject]s. Most of the time there will only be one
 * [ElmProject], but you might have multiple if you are working on multiple Elm apps in
 * the same IntelliJ project.
 *
 * There is only one workspace for the entire IntelliJ [Project].
 *
 * The state includes user-specific paths so it is persisted to IntelliJ's workspace file
 * (which is _not_ placed in version control).
 */
@State(name = "ElmWorkspace", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ElmWorkspaceService(
        val intellijProject: Project
) : PersistentStateComponent<Element> {


    init {
        with(intellijProject.messageBus.connect()) {
            subscribe(VirtualFileManager.VFS_CHANGES, ElmProjectWatcher {
                refreshAllProjects()
            })
        }
    }


    // Settings and Toolchain

    /* A nice view of the settings to the outside world */
    data class Settings(val toolchain: ElmToolchain?)

    /* Representation of settings suitable for editor UI and serialization */
    data class RawSettings(val binDirPath: String? = null)


    val settings: Settings
        get() {
            val raw = rawSettingsRef.get()
            return Settings(toolchain = raw.binDirPath?.let { ElmToolchain(it) })
        }


    val rawSettings get() = rawSettingsRef.get()


    private val rawSettingsRef = AtomicReference(RawSettings())


    fun modifySettings(notify: Boolean = true, f: (RawSettings) -> RawSettings): RawSettings {
        return rawSettingsRef.getAndUpdate(f)
                .also { if (notify) notifyDidChangeToolchain() }
    }


    private fun notifyDidChangeToolchain() {
        if (intellijProject.isDisposed) return
        intellijProject.messageBus.syncPublisher(TOOLCHAIN_TOPIC).toolchainChanged()
        // TODO [kl] should we also refresh the projects?
        // or combine this with projects-changed notification? It would be silly to refresh twice.
    }


    fun useToolchain(toolchain: ElmToolchain?) {
        modifySettings { it.copy(binDirPath = toolchain?.binDirPath.toString()) }
    }


    fun showConfigureToolchainUI() {
        ShowSettingsUtil.getInstance()
                .editConfigurable(intellijProject, ElmWorkspaceConfigurable(intellijProject))
    }


    // Elm Projects


    // IMPORTANT: must ensure thread-safe access
    private val projectsRef = AtomicReference(emptyList<ElmProject>())


    val allProjects: List<ElmProject>
        get() = projectsRef.get()


    private fun modifyProjects(notify: Boolean = true,
                               f: (List<ElmProject>) -> List<ElmProject>): List<ElmProject> {
        projectsRef.getAndUpdate(f)
        directoryIndex.resetIndex()
        if (notify)
            notifyDidChangeProjects()
        return allProjects
    }


    private fun notifyDidChangeProjects() {
        if (intellijProject.isDisposed) return
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                ProjectRootManagerEx.getInstanceEx(intellijProject)
                        .makeRootsChange(EmptyRunnable.getInstance(), false, true)
            }
        }
        intellijProject.messageBus.syncPublisher(WORKSPACE_TOPIC)
                .projectsUpdated(allProjects)
    }


    @Throws(ProjectLoadException::class)
    fun attachElmProject(manifestPath: Path) {
        if (allProjects.count() != 0) {
            throw ProjectLoadException("Multiple Elm projects are not yet supported. "
                    + "You must remove the existing project before adding this one.")
        }

        modifyProjects {
            doRefresh(it + loadProject(manifestPath))
        }
    }


    fun detachElmProject(manifestPath: Path? = null) {
        if (manifestPath == null) {
            modifyProjects { emptyList() }
        } else {
            modifyProjects { oldProjects ->
                oldProjects.filter { it.manifestPath != manifestPath }
            }
        }
    }


    fun refreshAllProjects(): List<ElmProject> =
            modifyProjects { doRefresh(it) }


    @Throws(ProjectLoadException::class)
    fun discoverAndRefresh(): List<ElmProject> {
        if (hasAtLeastOneValidProject())
            return allProjects

        val guessManifest = intellijProject.modules
                .asSequence()
                .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
                .mapNotNull { it.findChild(ElmToolchain.ELM_JSON) }
                .firstOrNull()
                ?: return allProjects

        return try {
            val newProject = loadProject(guessManifest.pathAsPath)
            modifyProjects { doRefresh(it + newProject) }
        } catch (e: ProjectLoadException) {
            allProjects
        }
    }


    fun hasAtLeastOneValidProject() =
            allProjects.any { it.manifestPath.exists() }


    // It's very important that this function has no side-effects, as it may be
    // invoked multiple times when attempting to update the [AtomicReference]
    // holding the list of projects.
    private fun doRefresh(projects: List<ElmProject>) =
            projects.mapNotNull { loadProjectSafely(it.manifestPath) }


    private fun loadProject(manifestPath: Path): ElmProject {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(manifestPath.toString())
                ?: throw ProjectLoadException("Could not find file $manifestPath")

        val toolchain = settings.toolchain
                ?: throw ProjectLoadException("Elm toolchain not configured")

        return ElmProject.parse(file.inputStream, manifestPath, toolchain)
    }


    private fun loadProjectSafely(manifestPath: Path): ElmProject? {
        return try {
            loadProject(manifestPath)
        } catch (e: ProjectLoadException) {
            log.warn("Failed to load Elm project at $manifestPath")
            val errorHTML = "Failed to load Elm project at $manifestPath:<br>${e.message}"
            intellijProject.showBalloon(errorHTML, NotificationType.ERROR)
            null
        }
    }


    // Lookup


    fun findProjectForFile(file: VirtualFile): ElmProject? =
            directoryIndex.getInfoForFile(file).takeIf { it !== noProjectSentinel }


    private val directoryIndex: LightDirectoryIndex<ElmProject> =
            LightDirectoryIndex(intellijProject, noProjectSentinel, Consumer { index ->
                val visited = mutableSetOf<VirtualFile>()
                fun put(file: VirtualFile?, elmProject: ElmProject) {
                    if (file == null || file in visited) return
                    visited += file
                    index.putInfo(file, elmProject)
                }

                for (project in allProjects) {
                    put(LocalFileSystem.getInstance().findFileByPath(project.projectDirPath), project)
                    // TODO [kl] re-visit this when we allow projects within projects
                    // We probably will need to register additional directories based
                    // on how [LightDirectoryIndex] walks up the directory tree.
                }
            })


    // Persistent State


    override fun getState(): Element {
        val state = Element("state")

        val projectsElement = Element("elmProjects")
        state.addContent(projectsElement)
        for (project in allProjects) {
            val elem = Element("project").setAttribute("path", project.manifestPath.systemIndependentPath)
            projectsElement.addContent(elem)
        }

        val settingsElement = Element("settings")
        state.addContent(settingsElement)
        val raw = rawSettingsRef.get()
        settingsElement.setAttribute("binDirPath", raw.binDirPath ?: "")

        return state
    }


    override fun loadState(state: Element) {
        // Must load the Settings before the Elm Projects in order to have an ElmToolchain ready
        val settingsElement = state.getChild("settings")
        val binDirPath = settingsElement.getAttributeValue("binDirPath").takeIf { it.isNotBlank() }
        modifySettings(notify = false) { RawSettings(binDirPath = binDirPath) }

        val loadedProjects = state.getChild("elmProjects").getChildren("project")
                .mapNotNull { it.getAttributeValue("path") }
                .mapNotNull { loadProjectSafely(Paths.get(it)) }
        modifyProjects(notify = false) { loadedProjects }

        // At this point the IntelliJ Project is still being opened, and we don't want
        // to notify any listeners too early. So we will do it in a subsequent run of
        // the event loop.
        ApplicationManager.getApplication().invokeLater {
            notifyDidChangeProjects()
            notifyDidChangeToolchain()
        }
    }


    // Misc


    interface ElmWorkspaceListener {
        fun projectsUpdated(projects: Collection<ElmProject>)
    }


    interface ElmToolchainListener {
        fun toolchainChanged()
    }


    companion object {
        val WORKSPACE_TOPIC = Topic("Elm workspace changes", ElmWorkspaceListener::class.java)
        val TOOLCHAIN_TOPIC = Topic("Elm toolchain changes", ElmToolchainListener::class.java)
    }
}


class ProjectLoadException(msg: String, cause: Exception? = null) : RuntimeException(msg, cause)


fun guessAndSetupElmProject(project: Project, explicitRequest: Boolean = false): Boolean {
    if (!explicitRequest) {
        val alreadyTried = run {
            val key = "org.elm.workspace.PROJECT_DISCOVERY"
            with(PropertiesComponent.getInstance(project)) {
                getBoolean(key).also { setValue(key, true) }
            }
        }
        if (alreadyTried) return false
    }

    val toolchain = project.elmToolchain
    if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
        discoverToolchain(project)
        // return now because `discoverToolchain` will, at some later time, try to discover any Elm projects
        return true
    }
    if (!project.elmWorkspace.hasAtLeastOneValidProject()) {
        project.elmWorkspace.discoverAndRefresh()
        return true
    }
    return false
}


private fun discoverToolchain(project: Project) {
    val toolchain = ElmToolchain.suggest(project)
            ?: return

    ApplicationManager.getApplication().invokeLater {
        if (project.isDisposed)
            return@invokeLater

        if (project.elmToolchain?.looksLikeValidToolchain() == true)
            return@invokeLater

        runWriteAction {
            project.elmWorkspace.useToolchain(toolchain)
        }

        project.showBalloon("Using Elm at ${toolchain.presentableLocation}", NotificationType.INFORMATION)
        project.elmWorkspace.discoverAndRefresh()
    }

}


// Convenience Extensions


val Project.elmWorkspace
    get() = service<ElmWorkspaceService>()


val Project.elmSettings
    get() = elmWorkspace.settings


val Project.elmToolchain: ElmToolchain?
    get() = elmSettings.toolchain


val Project.hasAnElmProject: Boolean
    get() = elmWorkspace.allProjects.isNotEmpty()
