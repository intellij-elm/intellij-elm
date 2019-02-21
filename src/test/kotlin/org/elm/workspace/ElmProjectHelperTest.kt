package org.elm.workspace

import org.elm.TestProject
import org.elm.fileTree
import org.elm.openapiext.pathAsPath
import org.frawa.elmtest.core.ElmProjectHelper
import java.util.*
import java.util.Optional.empty
import kotlin.streams.toList

class ElmProjectHelperTest : ElmWorkspaceTestBase() {

    fun `test all names`() {
        testProject()

        checkEquals(Arrays.asList("a", "b"), ElmProjectHelper(project).allNames().toList())
    }

    fun `test all names, with tests`() {
        testProjectWithoutTests()

        checkEquals(Arrays.asList("a"), ElmProjectHelper(project).allNames().toList())
    }

    fun `test by index`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        checkEquals(Optional.of(root.resolve("a").toString()), ElmProjectHelper(project).projectDirPathByIndex(0))
        checkEquals(Optional.of(root.resolve("b").toString()), ElmProjectHelper(project).projectDirPathByIndex(1))
        checkEquals(empty<String>(), ElmProjectHelper(project).projectDirPathByIndex(13))
    }

    fun `test by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        checkEquals(Optional.of("a"), ElmProjectHelper(project).nameByProjectDirPath(root.resolve("a").toString()))
        checkEquals(Optional.of("b"), ElmProjectHelper(project).nameByProjectDirPath(root.resolve("b").toString()))
        checkEquals(empty<String>(), ElmProjectHelper(project).nameByProjectDirPath(root.resolve("Toto").toString()))
    }

    private fun testProject(): TestProject {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm", "")
                }
                dir("tests") {
                }
            }
            dir("b") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("MainB.elm", "")
                }
                dir("tests") {
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("b/elm.json")).get()
        }

        return testProject
    }

    private fun testProjectWithoutTests(): TestProject {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm", "")
                }
                dir("tests") {
                }
            }
            dir("c-without") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("c-without/elm.json")).get()
        }

        return testProject
    }

    val elmJson = """
                        {
                            "type": "application",
                            "source-directories": [ "src" ],
                            "elm-version": "0.19.0",
                            "dependencies": {
                                "direct": {
                                },
                                "indirect": {
                                }
                             },
                            "test-dependencies": {
                                "direct": {
                                },
                                "indirect": {
                                }
                            }
                        }
                        """

}
