package org.elmSlowTests

import org.elm.fileTree
import org.elm.openapiext.elementFromXmlString
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.toXmlString
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace

class ElmWorkspaceServiceTest : ElmWorkspaceTestBase() {


    fun `test finds Elm project for source file`() {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                      "type": "application",
                      "source-directories": [ "src" ],
                      "elm-version": "0.19.0",
                      "dependencies": { },
                      "test-dependencies": {},
                      "do-not-edit-this-by-hand": {
                        "transitive-dependencies": { }
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

    fun `test auto discover Elm project at root level`() {
        val testProject = fileTree {
            project("elm.json", """
                {
                  "type": "application",
                  "source-directories": [ "src" ],
                  "elm-version": "0.19.0",
                  "dependencies": { },
                  "test-dependencies": {},
                  "do-not-edit-this-by-hand": {
                    "transitive-dependencies": { }
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
                      "dependencies": { },
                      "test-dependencies": {},
                      "do-not-edit-this-by-hand": {
                        "transitive-dependencies": { }
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
        val xml = """
            <state>
              <elmProjects>
                <project path="$rootPath/a/elm.json" />
              </elmProjects>
              <settings binDirPath="/usr/local/bin" />
            </state>
            """.trimIndent()

        // ... must be able to load from serialized state ...
        workspace.loadState(elementFromXmlString(xml))
        val actualProjects = workspace.allProjects.map { it.manifestPath.toString() }
        val expectedProjects = listOf("$rootPath/a/elm.json")
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
