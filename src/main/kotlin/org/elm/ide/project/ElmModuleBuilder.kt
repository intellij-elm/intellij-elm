package org.elm.ide.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VfsUtil
import org.elm.openapiext.pathAsPath
import org.elm.workspace.asyncAutoDiscoverWorkspace
import org.elm.workspace.elmToolchain
import org.elm.workspace.elmWorkspace
import org.intellij.lang.annotations.Language

private val log = logger<ElmModuleBuilder>()


/**
 * Constructs a new IntelliJ [Module], which is quite different from a module in Elm.
 */
class ElmModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> =
            ElmModuleType.INSTANCE

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean =
            true // we don't care about IntelliJ's concept of SDK

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        log.info("Setting up a new Elm content root")
        /*
            NOTE: this is called on the EDT and we already have a write action
         */
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
        modifiableRootModel.inheritSdk() // we don't care about SDKs, but IntelliJ does
        val project = modifiableRootModel.project

        VfsUtil.saveText(root.createChildData(this, "elm.json"), elmJson)
        val srcDir = root.createChildDirectory(this, "src")
        VfsUtil.saveText(srcDir.createChildData(this, "Main.elm"), elmMain)

        // By this point, the important stuff has finished. Now we will
        // try to auto-discover and configure some things for the user as
        // a convenience. If it doesn't work, no big deal: the user will be
        // prompted later to complete configuration.

        if (project.elmToolchain == null) {
            log.debug("Begin auto-discover the toolchain")
            try {
                asyncAutoDiscoverWorkspace(project, explicitRequest = true).get()
            } catch (e: Exception) {
                log.error("Auto-discover toolchain failed", e)
            }
            log.debug("Finished auto-discover: toolchain=${project.elmToolchain}")
        }

        if (project.elmToolchain != null) {
            log.debug("Attempting to attach the Elm project")
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
        "elm-version": "0.19.0",
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