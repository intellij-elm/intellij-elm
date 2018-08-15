package org.elmSlowTests

import com.intellij.history.core.Paths
import org.elm.fileTree
import org.elm.openapiext.elementFromXmlString
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.toXmlString
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace
import java.io.File

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
            attachElmProject(rootPath.resolve("a/elm.json"))
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


    fun `test can deserialize application json files`() {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [ "src" ],
                        "elm-version": "0.19.0",
                        "dependencies": {
                            "direct": {
                                "elm/core": "1.0.0",
                                "elm/html": "1.0.0"
                            },
                            "indirect": {
                                "elm/virtual-dom": "1.0.0"
                            }
                         },
                        "test-dependencies": {
                            "direct": {
                                "elm-explorations/test": "1.0.0"
                            },
                            "indirect": {
                                "eeue56/elm-lazy": "1.0.1",
                                "eeue56/elm-shrink": "2.0.0"
                            }
                        }
                    }
                    """)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            attachElmProject(rootPath.resolve("a/elm.json"))
        }

        val dto = workspace.allProjects.first()

        checkEquals("0.19.0", dto.elmVersion)

        checkEquals(setOf(
                "elm/core" to "1.0.0",
                "elm/html" to "1.0.0",
                "elm/virtual-dom" to "1.0.0"
        ), dto.dependencies.map { Pair(it.name, it.version) }.toSet())

        checkEquals(setOf(
                "elm-explorations/test" to "1.0.0",
                "eeue56/elm-lazy" to "1.0.1",
                "eeue56/elm-shrink" to "2.0.0"
        ), dto.testDependencies.map { Pair(it.name, it.version) }.toSet())
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

        val elmProjects = project.elmWorkspace.discoverAndRefresh()
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

        val elmProjects = project.elmWorkspace.discoverAndRefresh()
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
        workspace.loadState(elementFromXmlString(xml))
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
