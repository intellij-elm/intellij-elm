package org.elm.workspace

import org.elm.TestProject
import org.elm.fileTree
import org.elm.openapiext.pathAsPath
import org.frawa.elmtest.core.ElmProjectTestsHelper
import java.util.*
import java.util.Optional.empty
import kotlin.streams.toList

class ElmProjectHelperTest : ElmWorkspaceTestBase() {

    fun `test all names`() {
        testProject()

        checkEquals(Arrays.asList("a", "b"), ElmProjectTestsHelper(project).allNames().toList())
    }

    fun `test by name`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        checkEquals(Optional.of(root.resolve("a").toString()), ElmProjectTestsHelper(project).projectDirPathByName("a"))
        checkEquals(Optional.of(root.resolve("b").toString()), ElmProjectTestsHelper(project).projectDirPathByName("b"))
        checkEquals(empty<String>(), ElmProjectTestsHelper(project).projectDirPathByName("gnu"))
        checkEquals(empty<String>(), ElmProjectTestsHelper(project).projectDirPathByName("without-tests"))
    }

    fun `test by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        checkEquals(Optional.of("a"), ElmProjectTestsHelper(project).nameByProjectDirPath(root.resolve("a").toString()))
        checkEquals(Optional.of("b"), ElmProjectTestsHelper(project).nameByProjectDirPath(root.resolve("b").toString()))
        checkEquals(empty<String>(), ElmProjectTestsHelper(project).nameByProjectDirPath(root.resolve("Toto").toString()))
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
            dir("without-tests") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
            dir("b") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm", "")
                }
                dir("tests") {
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("without-tests/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("b/elm.json")).get()
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
