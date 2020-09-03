package org.elm.ide.project.web;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.ide.util.projectWizard.WebProjectTemplate
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil.join
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import org.elm.ide.icons.ElmIcons
import org.elm.ide.project.elmJson
import org.elm.ide.project.elmMain
import org.elm.openapiext.runWriteCommandAction
import org.elm.workspace.DEFAULT_TESTS_DIR_NAME
import org.elm.workspace.asyncAutoDiscoverWorkspace

private val log = logger<ElmWebProjectTemplate>()

class ElmWebProjectTemplate : WebProjectTemplate<Any>(), CustomStepProjectGenerator<Any> {

    override fun createStep(
            projectGenerator: DirectoryProjectGenerator<Any>?,
            callback: AbstractNewProjectStep.AbstractCallback<Any>?
    ): AbstractActionWithPanel {
        return ProjectSettingsStepBase(this, callback)
    }

    override fun getIcon() = ElmIcons.COLORFUL
    override fun getName() = "Elm"
    override fun getDescription(): String? = null

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: Any, module: Module) {
        log.debug("Generating a new project")
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
            val rootModel = ModuleRootManager.getInstance(module).modifiableModel
            val contentEntry = rootModel.contentEntries.single()
            val root = contentEntry.file ?: return@runWhenProjectIsInitialized

            project.runWriteCommandAction {
                // Generate the project skeleton
                VfsUtil.saveText(root.createChildData(this, "elm.json"), elmJson)
                val srcDir = root.createChildDirectory(this, "src")
                VfsUtil.saveText(srcDir.createChildData(this, "Main.elm"), elmMain)

                // Mark the source roots, etc.
                with(contentEntry) {
                    addSourceFolder(join(root.url, "src"), /* test = */ false)
                    addSourceFolder(join(root.url, DEFAULT_TESTS_DIR_NAME), /* test = */ true)
                    addExcludeFolder(join(root.url, "elm-stuff"))
                }

                rootModel.commit()
                project.save()
            }

            // attempt to auto-configure the Elm toolchain and attach `elm.json`.
            // TODO [kl] in the future we may want to show UI here (via `createPeer()` override)
            //           to allow the user explicitly configure the Elm paths, etc.
            asyncAutoDiscoverWorkspace(project, explicitRequest = true)
        }
    }
}