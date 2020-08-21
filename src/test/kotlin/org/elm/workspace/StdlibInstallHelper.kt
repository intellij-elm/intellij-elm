package org.elm.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.elm.fileTree
import org.elm.openapiext.pathAsPath
import org.elm.workspace.ElmToolchain.Companion.ELM_JSON
import org.intellij.lang.annotations.Language

/*
Some of the Elm tests depend on having certain Elm packages installed in the global location.
And since Elm has pretty poor facilities for installing packages, we have to resort to tricks
like asking it to compile a dummy application.
 */

interface ElmStdlibVariant {

    /**
     * A description of the packages that we want to be installed
     */
    val jsonManifest: String

    /**
     * Install Elm 0.19 stdlib in the default location ($HOME/.elm)
     */
    fun ensureElmStdlibInstalled(project: Project, toolchain: ElmToolchain) {
        val elmCLI = toolchain.elmCLI
                ?: error("Must have a path to the Elm compiler to install Elm stdlib")

        val compilerVersion = elmCLI.queryVersion().orNull()
                ?: error("Could not query the Elm compiler version")
        require(compilerVersion != Version(0, 18, 0))

        // Create the dummy Elm project on-disk (real file system) and invoke the Elm compiler on it.
        val onDiskTmpDir = LocalFileSystem.getInstance()
                .refreshAndFindFileByIoFile(FileUtil.createTempDirectory("elm-stdlib-variant", null, true))
                ?: error("Could not create on-disk temp dir for Elm stdlib installation")

        fileTree {
            project(ELM_JSON, jsonManifest)
            file("Main.elm", elmHeadlessWorkerCode)
        }.create(project, onDiskTmpDir)

        elmCLI.installDeps(project, onDiskTmpDir.pathAsPath.resolve("elm.json"))
    }
}

object EmptyElmStdlibVariant : ElmStdlibVariant {
    override fun ensureElmStdlibInstalled(project: Project, toolchain: ElmToolchain) {
        // Don't do anything. The Elm compiler would refuse to use this manifest
        // since it's missing `elm/core` and other required packages. Also, it's
        // faster if we short-circuit things here rather than invoking the Elm
        // compiler in an external process.
    }

    override val jsonManifest: String
        @Language("JSON")
        get() = """
            {
                "type": "application",
                "source-directories": [
                    "."
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {},
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent()
}

/**
 * Describes a "minimal" installation of the Elm stdlib. This is the bare minimum
 * required to compile an Elm application.
 */
object MinimalElmStdlibVariant : ElmStdlibVariant {
    override val jsonManifest: String
        @Language("JSON")
        get() = """
            {
                "type": "application",
                "source-directories": [
                    "."
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/core": "1.0.0",
                        "elm/json": "1.0.0"
                    },
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent()
}


/**
 * Allows for ad-hoc installation of Elm packages given by [extraDependencies]
 */
class CustomElmStdlibVariant(val extraDependencies: Map<String, Version>) : ElmStdlibVariant {
    override val jsonManifest: String
        @Language("JSON")
        get() {
            val moreDirectDeps = extraDependencies
                    .map { (pkgName, version) ->
                        "\"${pkgName}\": \"${version}\""
                    }.joinToString(",\n")
            return """
                    {
                        "type": "application",
                        "source-directories": [
                            "."
                        ],
                        "elm-version": "0.19.1",
                        "dependencies": {
                            "direct": {
                                "elm/core": "1.0.0",
                                "elm/json": "1.0.0",
                                ${moreDirectDeps}
                            },
                            "indirect": {}
                        },
                        "test-dependencies": {
                            "direct": {},
                            "indirect": {}
                        }
                    }
                    """.trimIndent()
        }
}


/**
 * Describes a "full" installation of the Elm stdlib, including all of the stuff that you need for a normal
 * Elm app including test dependencies.
 */
object FullElmStdlibVariant : ElmStdlibVariant {
    override val jsonManifest: String
        @Language("JSON")
        get() = """
            {
                "type": "application",
                "source-directories": [
                    "."
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/core": "1.0.0",
                        "elm/html": "1.0.0",
                        "elm/json": "1.0.0",
                        "elm/time": "1.0.0"
                    },
                    "indirect": {
                        "elm/virtual-dom": "1.0.2"
                    }
                },
                "test-dependencies": {
                    "direct": {
                        "elm-explorations/test": "1.0.0"
                    },
                    "indirect": {
                        "elm/random": "1.0.0"
                    }
                }
            }
            """.trimIndent()
}


/**
 * A dummy Elm Main module necessary to get Elm to install the packages we want.
 */
private val elmHeadlessWorkerCode = """
        module Main exposing (..)
        main =
            Platform.worker { init = init , update = update , subscriptions = always Sub.none }

        init : () -> ( Int, Cmd Msg )
        init flags = ( 0, Cmd.none )

        type Msg = Nop

        update msg model = ( model, Cmd.none )
        """.trimIndent()
