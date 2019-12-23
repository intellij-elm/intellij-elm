package org.elm.ide.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import org.elm.openapiext.pathAsPath
import org.elm.utils.layout
import org.elm.workspace.asyncAutoDiscoverWorkspace
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace
import org.intellij.lang.annotations.Language
import javax.swing.JComponent

private val log = logger<ElmModuleBuilder>()


/**
 * Constructs a new IntelliJ [Module], which is quite different from what Elm considers a module.
 *
 * An IntelliJ project can have multiple modules. Each module is supposed to represent a piece
 * of software that can be built/run/etc. But IntelliJ's concepts of "module", "library" and "SDK"
 * are too highly tied to Java projects, so I've followed intellij-rust's example and decided
 * to avoid using IntelliJ's [Module] as much as possible.
 *
 * Nevertheless, an IntelliJ project must have at least one [Module], so we will configure one
 * when generating a new IntelliJ project, but only to appease the platform.
 *
 * LIFECYCLE:
 *
 * The control flow during IntelliJ's "new project" process is confusing. Here is how I understand it:
 *
 * 1. The user clicks "New Project"
 * 2. IntelliJ shows them a list of project types, Elm being one of them.
 * 3. When the user selects the Elm project type, IntelliJ calls [ElmProjectWizardStep.getComponent] and shows the UI.
 * 4. The user interacts with the UI and then clicks the "Next" button
 * 5. IntelliJ calls [ElmProjectWizardStep.updateDataModel]. Our code takes data from the UI and saves it somewhere.
 * 6. IntelliJ prompts the user to give the project a name and pick a location on the file system.
 * 7. IntelliJ calls [ElmModuleBuilder.setupRootModel]
 * 8. IntelliJ calls [ElmProjectWizardStep.ElmConfigUpdater.update]
 *
 *
 * REMINDER TO FUTURE SELF:
 *
 * Q) What about "Import Project"?
 *
 * A) You can hook into IntelliJ's "import project" by implementing the [ProjectStructureDetector] hook.
 *    But it adds some complexity for little gain. Its lifecycle has some subtle differences vs the
 *    "new project" flow. And there could be multiple project structure detectors contributing different
 *    modules for different language plugins, so you have to be slightly careful about that.
 *
 *    But the real reason why I decided against implementing it is that, at the end of the day, the user doesn't
 *    even have to go through "import project" to load an Elm project for the first time. They can always just
 *    choose "Open" and select a directory. IntelliJ will silently construct a new project without even calling the
 *    [ProjectStructureDetector]. So if you wanted to enforce some invariants about new/import project creation,
 *    tough-luck. This is why [org.elm.ide.notifications.ElmNeedsConfigNotificationProvider] is so important.
 *    It is the last line of defense for ensuring that the Elm plugin is properly configured.
 */
class ElmModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> =
            ElmModuleType.INSTANCE

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean =
            true // we don't care about IntelliJ's concept of SDK

    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep =
            ElmProjectWizardStep(context).apply {
                Disposer.register(parentDisposable, Disposable { this.disposeUIResources() })
            }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        log.info("Setting up a new Elm content root")
        doAddContentEntry(modifiableRootModel)
        modifiableRootModel.inheritSdk() // we don't care about SDKs, but IntelliJ does
    }
}

private class ElmProjectWizardStep(val context: WizardContext) : ModuleWizardStep() {

    override fun getComponent(): JComponent =
            layout {
                block("intellij-elm") {
                    noteRow("Click the 'Next' button to generate a new Elm project.")
                    noteRow("""GitHub: <a href="https://github.com/klazuka/intellij-elm">klazuka/intellij-elm</a>""")
                    noteRow("""Demo Video: <a href="https://www.youtube.com/watch?v=CC2TdNuZztI">https://www.youtube.com/watch?v=CC2TdNuZztI</a>""")
                }
            }

    override fun updateDataModel() {
        val projectBuilder = context.projectBuilder as? ElmModuleBuilder ?: return
        projectBuilder.addModuleConfigurationUpdater(ElmConfigUpdater)
    }

    override fun validate() = true

    private object ElmConfigUpdater : ModuleBuilder.ModuleConfigurationUpdater() {
        override fun update(module: Module, rootModel: ModifiableRootModel) {
            // Generate the project skeleton
            val contentEntry = rootModel.contentEntries.single()
            val root = contentEntry.file ?: return
            VfsUtil.saveText(root.createChildData(this, "elm.json"), elmJson)
            val srcDir = root.createChildDirectory(this, "src")
            VfsUtil.saveText(srcDir.createChildData(this, "Main.elm"), elmMain)

            // Mark the source roots, etc.
            with(contentEntry) {
                addSourceFolder(FileUtil.join(root.url, "src"),   /* test = */ false)
                addSourceFolder(FileUtil.join(root.url, "tests"), /* test = */ true)
                addExcludeFolder(FileUtil.join(root.url, "elm-stuff"))
            }

            // By this point, the important stuff has finished. Now we will
            // try to auto-discover and configure some things for the user as
            // a convenience. If it doesn't work, no big deal: the user will be
            // prompted later to complete configuration.

            val project = module.project
            if (!project.elmToolchain.looksLikeValidToolchain()) {
                log.debug("Begin auto-discover the toolchain")
                try {
                    asyncAutoDiscoverWorkspace(project, explicitRequest = true).get()
                } catch (e: Exception) {
                    log.error("Auto-discover toolchain failed", e)
                }
                log.debug("Finished auto-discover: toolchain=${project.elmToolchain}")
            }

            log.debug("Attempting to attach the Elm project")
            // `asyncAutoDiscoverWorkspace` *should* find and attach any `elm.json` files,
            // but at this point in the "new project" lifecycle, the IntelliJ Module has not
            // yet been added to the IntelliJ project. And since the auto-discover workspace
            // feature uses the content roots of each module in the project to recursively
            // search for `elm.json` files, it will not have found anything.
            //
            // TODO check if there's a better lifecycle hook so that we don't need to do this here.
            //
            // IMPORTANT: do *not* block on completion of the future (deadlock risk).
            project.elmWorkspace.asyncAttachElmProject(root.pathAsPath.resolve("elm.json"))
        }
    }

}

/**
 * As-of 2019-02-09, this is the standard `elm.json` file created by `elm init`.
 * I would just run `elm init`, but it prompts the user for input, and there is
 * no flag to override that behavior.
 *
 * TODO request that something like a `--yes` flag be added to `elm init` CLI
 */
@Language("JSON")
val elmJson = """
    {
        "type": "application",
        "source-directories": [
            "src"
        ],
        "elm-version": "0.19.1",
        "dependencies": {
            "direct": {
                "elm/browser": "1.0.1",
                "elm/core": "1.0.2",
                "elm/html": "1.0.0"
            },
            "indirect": {
                "elm/json": "1.1.2",
                "elm/time": "1.0.0",
                "elm/url": "1.0.0",
                "elm/virtual-dom": "1.0.2"
            }
        },
        "test-dependencies": {
            "direct": {},
            "indirect": {}
        }
    }""".trimIndent()


/**
 * An empty Elm application to get started with.
 */
@Language("Elm")
val elmMain = """
    module Main exposing (main)

    import Html exposing (text)

    main = text "hi"
    """.trimIndent()
