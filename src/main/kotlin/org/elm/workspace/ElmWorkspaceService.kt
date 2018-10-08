/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.workspace

import com.google.common.annotations.VisibleForTesting
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
import org.elm.openapiext.findFileBreadthFirst
import org.elm.openapiext.findFileByPath
import org.elm.openapiext.modules
import org.elm.openapiext.pathAsPath
import org.elm.utils.joinAll
import org.elm.utils.runAsyncTask
import org.elm.workspace.ElmToolchain.Companion.ELM_MANIFEST_FILE_NAMES
import org.elm.workspace.ui.ElmWorkspaceConfigurable
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
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
                asyncRefreshAllProjects()
            })
        }
    }


    // SETTINGS AND TOOLCHAIN

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


    /**
     * The core, internal function updating the workspace settings. All updates must ultimately
     * go through here to make sure that the appropriate notifications are triggered.
     */
    fun modifySettings(notify: Boolean = true, f: (RawSettings) -> RawSettings): RawSettings {
        return rawSettingsRef.getAndUpdate(f)
                .also { if (notify) notifyDidChangeWorkspace() }
    }


    fun useToolchain(toolchain: ElmToolchain?) {
        modifySettings { it.copy(binDirPath = toolchain?.binDirPath.toString()) }
    }


    fun showConfigureToolchainUI() {
        ShowSettingsUtil.getInstance()
                .editConfigurable(intellijProject, ElmWorkspaceConfigurable(intellijProject))
    }


    // ELM PROJECTS


    /**
     * The INTERNAL list of Elm projects in the workspace. Project truth lives here.
     *
     * IMPORTANT: must ensure thread-safe access and that the ElmProject objects themselves are immutable values.
     */
    private val projectsRef = AtomicReference(emptyList<ElmProject>())


    /**
     * The list of Elm projects in the workspace, suitable for use by the rest of the plugin.
     */
    val allProjects: List<ElmProject>
        get() = projectsRef.get()


    /**
     * The core, internal function for updating the list of Elm projects in the workspace.
     * All updates must ultimately go through here to make sure that dependent data-structures
     * are updated and notifications are triggered.
     */
    private fun modifyProjects(f: (List<ElmProject>) -> List<ElmProject>): List<ElmProject> {
        projectsRef.getAndUpdate(f)
        directoryIndex.resetIndex()
        notifyDidChangeWorkspace()
        return allProjects
    }


    /**
     * Add [elmProject] to the workspace. If the project has already been registered, it will be replaced
     * with the newer one.
     */
    private fun upsertProject(elmProject: ElmProject): List<ElmProject> =
            modifyProjects { projects ->
                val otherProjects = projects.filter { it.manifestPath != elmProject.manifestPath }
                otherProjects + elmProject
            }


    /**
     * Asynchronously load an Elm project described by a manifest file (e.g. `elm.json`).
     */
    private fun asyncLoadProject(manifestPath: Path): CompletableFuture<ElmProject> =
            runAsyncTask(intellijProject, "Loading Elm project '$manifestPath'") {
                val toolchain = settings.toolchain
                        ?: throw ProjectLoadException("Elm toolchain not configured")

                ElmProject.parse(manifestPath, toolchain)
            }.whenComplete { _, error ->
                // log the result
                if (error == null) {
                    log.info("Successfully loaded Elm project $manifestPath")
                } else {
                    when (error) {
                        is ProjectLoadException -> log.warn("Failed to load $manifestPath: ${error.message}")
                        else -> log.error("Unexpected error when loading $manifestPath", error)
                    }
                }
            }


    // WORKSPACE ACTIONS


    fun asyncAttachElmProject(manifestPath: Path): CompletableFuture<List<ElmProject>> =
            asyncLoadProject(manifestPath)
                    .thenApply {
                        upsertProject(it)
                    }


    fun detachElmProject(manifestPath: Path) {
        modifyProjects { oldProjects ->
            oldProjects.filter { it.manifestPath != manifestPath }
        }
    }


    fun asyncRefreshAllProjects(): CompletableFuture<List<ElmProject>> =
            allProjects.map { elmProject ->
                asyncLoadProject(elmProject.manifestPath)
                        .exceptionally { null } // TODO [kl] capture info about projects that failed to load and show to user
            }.joinAll()
                    .thenApply { rawProjects ->
                        val freshProjects = rawProjects.filterNotNull().associateBy { it.manifestPath }
                        modifyProjects { currentProjects ->
                            // replace existing projects with the fresh ones, if possible
                            currentProjects.map {
                                freshProjects[it.manifestPath] ?: it
                            }
                        }
                    }


    fun asyncDiscoverAndRefresh(): CompletableFuture<List<ElmProject>> {
        if (hasAtLeastOneValidProject())
            return CompletableFuture.completedFuture(allProjects)
        val guessManifest = intellijProject.modules
                .asSequence()
                .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
                .mapNotNull { dir -> dir.findFileBreadthFirst { it.name in ELM_MANIFEST_FILE_NAMES } }
                .firstOrNull()
                ?: return CompletableFuture.completedFuture(allProjects)

        return asyncLoadProject(guessManifest.pathAsPath)
                .thenApply {
                    upsertProject(it)
                }.exceptionally { emptyList() }
    }


    fun hasAtLeastOneValidProject() =
            allProjects.any { it.manifestPath.exists() }


    // PROJECT LOOKUP


    fun findProjectForFile(file: VirtualFile): ElmProject? =
            directoryIndex.getInfoForFile(file).takeIf { it !== noProjectSentinel }


    private val directoryIndex: LightDirectoryIndex<ElmProject> =
            LightDirectoryIndex(intellijProject, noProjectSentinel, Consumer { index ->
                val visited = mutableSetOf<VirtualFile>()
                fun put(path: Path?, elmProject: ElmProject) {
                    if (path == null) return
                    val file = LocalFileSystem.getInstance().findFileByPath(path)
                    if (file == null || file in visited) return
                    visited += file
                    index.putInfo(file, elmProject)
                }

                for (project in allProjects) {
                    for (sourceDir in project.sourceDirectories) {
                        put(project.projectDirPath.resolve(sourceDir), project)
                    }
                    for (pkg in project.allResolvedDependencies) {
                        put(pkg.projectDirPath, pkg)
                    }
                }
            })


    // INTEGRATION TEST SUPPORT


    fun setupForTests(toolchain: ElmToolchain, elmProject: ElmProject) {
        useToolchain(toolchain)
        upsertProject(elmProject)
    }


    // PERSISTENT STATE


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
        asyncLoadState(state)
    }

    @VisibleForTesting
    // TODO [kl] make this `internal` visibility...
    // I can't do it right now because there's something wrong with Gradle where it treats the test source set
    // as not belonging to the same Kotlin module as the code-under-test. But according to
    // https://kotlinlang.org/docs/reference/visibility-modifiers.html#modules it *should* work!
    fun asyncLoadState(state: Element): CompletableFuture<Unit> {
        // Must load the Settings before the Elm Projects in order to have an ElmToolchain ready
        val settingsElement = state.getChild("settings")
        val binDirPath = settingsElement.getAttributeValue("binDirPath").takeIf { it.isNotBlank() }
        modifySettings(notify = false) { RawSettings(binDirPath = binDirPath) }

        // Ensure that `elm-stuff` directories are always excluded so that they don't pollute open-by-filename, etc.
        intellijProject.modules
                .asSequence()
                .flatMap { ModuleRootManager.getInstance(it).contentEntries.asSequence() }
                .forEach {
                    if ("elm-stuff" !in it.excludePatterns)
                        it.addExcludePattern("elm-stuff")
                }

        return state.getChild("elmProjects")
                .getChildren("project")
                .mapNotNull { it.getAttributeValue("path") }
                .mapNotNull { Paths.get(it) }
                .map { path ->
                    asyncLoadProject(path)
                            .exceptionally { null } // TODO [kl] capture info about projects that failed to load and show to user
                }.joinAll()
                .thenApply { rawProjects ->
                    modifyProjects { _ -> rawProjects.filterNotNull() }
                    Unit
                }
    }


    // NOTIFICATIONS


    private fun notifyDidChangeWorkspace() {
        if (intellijProject.isDisposed) return
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                ProjectRootManagerEx.getInstanceEx(intellijProject)
                        .makeRootsChange(EmptyRunnable.getInstance(), false, true)
            }
        }
        intellijProject.messageBus.syncPublisher(WORKSPACE_TOPIC)
                .didUpdate()
    }


    interface ElmWorkspaceListener {
        fun didUpdate()
    }


    companion object {
        val WORKSPACE_TOPIC = Topic("Elm workspace changes", ElmWorkspaceListener::class.java)
    }
}


class ProjectLoadException(msg: String, cause: Exception? = null) : RuntimeException(msg, cause)


// AUTO-DISCOVER


fun asyncAutoDiscoverWorkspace(project: Project, explicitRequest: Boolean = false): CompletableFuture<Unit> {
    if (!explicitRequest) {
        val alreadyTried = run {
            val key = "org.elm.workspace.PROJECT_DISCOVERY"
            with(PropertiesComponent.getInstance(project)) {
                getBoolean(key).also { setValue(key, true) }
            }
        }
        if (alreadyTried) return CompletableFuture.completedFuture(Unit)
    }

    val toolchain = project.elmToolchain?.takeIf { it.looksLikeValidToolchain() }
    if (toolchain == null) {
        ElmToolchain.suggest(project)
                ?.let {
                    project.elmWorkspace.useToolchain(it)
                    project.showBalloon("Using Elm at ${it.presentableLocation}", NotificationType.INFORMATION)
                }
    }

    return if (!project.elmWorkspace.hasAtLeastOneValidProject()) {
        project.elmWorkspace.asyncDiscoverAndRefresh().thenApply { Unit }
    } else {
        CompletableFuture.completedFuture(Unit)
    }
}


// CONVENIENCE EXTENSIONS


val Project.elmWorkspace
    get() = service<ElmWorkspaceService>()


val Project.elmSettings
    get() = elmWorkspace.settings


val Project.elmToolchain: ElmToolchain?
    get() = elmSettings.toolchain
