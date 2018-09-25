package org.elm.workspace

import junit.framework.TestCase
import org.elm.fileTree
import org.elm.openapiext.elementFromXmlString
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.toXmlString
import java.nio.file.Paths

class ElmWorkspaceServiceTest : ElmWorkspaceTestBase() {


    fun `test finds Elm project for source file`() {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                      "type": "application",
                      "source-directories": [ "src" ],
                      "elm-version": "0.19.0",
                      "dependencies": {
                        "direct": {},
                        "indirect": {}
                      },
                      "test-dependencies": {
                        "direct": {},
                        "indirect": {}
                      }
                    }
                    """)
                dir("src") {
                    elm("Main.elm", "")
                    elm("Utils.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
        }

        fun checkFile(relativePath: String, projectName: String?) {
            val vFile = testProject.root.findFileByRelativePath(relativePath)!!
            val project = workspace.findProjectForFile(vFile)
            if (project?.presentableName != projectName) {
                error("Expected $projectName, found $project for $relativePath")
            }
        }

        checkFile("a/src/Main.elm", "a")
        checkFile("a/src/Utils.elm", "a")
    }


    fun `test can attach application json files`() {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [ "src", "vendor" ],
                        "elm-version": "0.19.0",
                        "dependencies": {
                            "direct": {
                                "elm/core": "1.0.0",
                                "elm/html": "1.0.0"
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
                    """)
                dir("src") {
                    elm("Main.elm", "")
                }
                dir("vendor") {
                    elm("VendoredPackage.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
        }

        val elmProject = workspace.allProjects.firstOrNull()
        if (elmProject == null) {
            TestCase.fail("failed to find an Elm project")
            return
        }

        if (elmProject !is ElmApplicationProject) {
            TestCase.fail("expected an Elm application project, got $elmProject")
            return
        }

        checkEquals(Version(0, 19, 0), elmProject.elmVersion)
        checkEquals(setOf(Paths.get("src"), Paths.get("vendor")), elmProject.sourceDirectories.toSet())

        checkEquals(setOf(
                "elm/core" to Version(1, 0, 0),
                "elm/html" to Version(1, 0, 0),
                "elm/virtual-dom" to Version(1, 0, 2)
        ), elmProject.dependencies.map { it.name to it.version }.toSet())

        checkEquals(setOf(
                "elm-explorations/test" to Version(1, 0, 0),
                "elm/random" to Version(1, 0, 0)
        ), elmProject.testDependencies.map { it.name to it.version }.toSet())
    }

    fun `test can attach package json files`() {
        val testProject = fileTree {
            project("elm.json", """
                    {
                        "type": "package",
                        "name": "elm/json",
                        "summary": "Encode and decode JSON values",
                        "license": "BSD-3-Clause",
                        "version": "1.0.0",
                        "exposed-modules": [
                            "Json.Decode",
                            "Json.Encode"
                        ],
                        "elm-version": "0.19.0 <= v < 0.20.0",
                        "dependencies": {
                            "elm/core": "1.0.0 <= v < 2.0.0"
                        },
                        "test-dependencies": {
                            "elm-explorations/test": "1.0.0 <= v < 2.0.0"
                        }
                    }
                    """)
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("elm.json")).get()
        }

        val elmProject = workspace.allProjects.firstOrNull()
        if (elmProject == null) {
            TestCase.fail("failed to find an Elm project")
            return
        }

        if (elmProject !is ElmPackageProject) {
            TestCase.fail("expected an Elm package project, got $elmProject")
            return
        }

        checkEquals(makeConstraint(Version(0, 19, 0), Version(0, 20, 0))
                , elmProject.elmVersion)

        // The source directory for Elm 0.19 packages is implicitly "src". It cannot be changed.
        checkEquals(setOf(Paths.get("src")), elmProject.sourceDirectories.toSet())

        checkEquals(setOf("elm/core" to Version(1, 0, 0)),
                elmProject.dependencies.map { it.name to it.version }.toSet())

        checkEquals(setOf("elm-explorations/test" to Version(1, 0, 0)),
                elmProject.testDependencies.map { it.name to it.version }.toSet())

        checkEquals(setOf("Json.Decode", "Json.Encode"),
                elmProject.exposedModules.toSet())
    }

    fun `test can attach package json file with annotated exposed modules`() {
        val testProject = fileTree {
            project("elm.json", """
                    {
                        "type": "package",
                        "name": "elm/json",
                        "summary": "Encode and decode JSON values",
                        "license": "BSD-3-Clause",
                        "version": "1.0.0",
                        "exposed-modules": {
                            "Decoding": [ "Json.Decode"],
                            "Encoding": [ "Json.Encode"]
                        },
                        "elm-version": "0.19.0 <= v < 0.20.0",
                        "dependencies": {
                            "elm/core": "1.0.0 <= v < 2.0.0"
                        },
                        "test-dependencies": {
                            "elm-explorations/test": "1.0.0 <= v < 2.0.0"
                        }
                    }
                    """)
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("elm.json")).get()
        }

        val elmProject = workspace.allProjects.firstOrNull()
        if (elmProject == null) {
            TestCase.fail("failed to find an Elm project")
            return
        }

        if (elmProject !is ElmPackageProject) {
            TestCase.fail("expected an Elm package project, got $elmProject")
            return
        }

        checkEquals(setOf("Json.Decode", "Json.Encode"),
                elmProject.exposedModules.toSet())
    }

    fun `test auto discover Elm project at root level`() {
        val testProject = fileTree {
            project("elm.json", """
                {
                  "type": "application",
                  "source-directories": [ "src" ],
                  "elm-version": "0.19.0",
                  "dependencies": {
                    "direct": {},
                    "indirect": {}
                  },
                  "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                  }
                }
                """)
            dir("src") {
                elm("Main.elm", "")
            }
        }.create(project, elmWorkspaceDirectory)

        val elmProjects = project.elmWorkspace.asyncDiscoverAndRefresh().get()
        check(elmProjects.size == 1) { "Should have found one Elm project but found ${elmProjects.size}" }
        val elmProject = elmProjects.first()
        check(elmProject.manifestPath == testProject.root.pathAsPath.resolve("elm.json"))
    }

    fun `test auto discover Elm project skips bad project files`() {
        fileTree {
            project("elm.json", """ { "BOGUS": "INVALID ELM.JSON" } """)
            dir("src") {
                elm("Main.elm", "")
            }
        }.create(project, elmWorkspaceDirectory)

        val elmProjects = project.elmWorkspace.asyncDiscoverAndRefresh().get()
        check(elmProjects.isEmpty()) { "Should have found zero Elm projects but found ${elmProjects.size}" }
    }

    fun `test persistence`() {
        // setup real files on disk
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                      "type": "application",
                      "source-directories": [ "src" ],
                      "elm-version": "0.19.0",
                      "dependencies": {
                        "direct": {},
                        "indirect": {}
                      },
                      "test-dependencies": {
                        "direct": {},
                        "indirect": {}
                      }
                    }
                    """)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val workspace = project.elmWorkspace
        val rootPath = testProject.root.pathAsPath

        // The known-good, serialized state that we must be able to handle
        val projectPath = rootPath.resolve("a").resolve("elm.json")
        val projectPathString = projectPath.toString().replace("\\", "/") // normalize windows paths
        val xml = """
            <state>
              <elmProjects>
                <project path="$projectPathString" />
              </elmProjects>
              <settings binDirPath="/usr/local/bin" />
            </state>
            """.trimIndent()

        // ... must be able to load from serialized state ...
        workspace.asyncLoadState(elementFromXmlString(xml)).get()
        val actualProjects = workspace.allProjects.map { it.manifestPath }
        val expectedProjects = listOf(projectPath)
        checkEquals(expectedProjects, actualProjects)

        // ... and serialize the resulting state ...
        val actualXml = workspace.state.toXmlString()
        checkEquals(xml, actualXml)
    }


    fun `test persistence with empty toolchain binDirPath`() {
        val workspace = project.elmWorkspace

        // The missing binDirPath is treated as the empty string
        val xml = """
            <state>
              <elmProjects />
              <settings binDirPath="" />
            </state>
            """.trimIndent()

        // ... must be able to load from serialized state ...
        workspace.loadState(elementFromXmlString(xml))

        // ... and serialize the resulting state ...
        val actualXml = workspace.state.toXmlString()
        checkEquals(xml, actualXml)
    }
}


private fun makeConstraint(low: Version, high: Version): Constraint {
    return Constraint(
            low = low,
            lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
            highOp = Constraint.Op.LESS_THAN,
            high = high
    )
}

