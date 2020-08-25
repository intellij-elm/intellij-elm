package org.elm.workspace

import org.elm.TestProject
import org.elm.fileTree
import org.elm.ide.test.core.ElmProjectTestsHelper
import org.elm.openapiext.pathAsPath

class ElmProjectHelperTest : ElmWorkspaceTestBase() {

    fun `test all names`() {
        testProject()

        val helper = ElmProjectTestsHelper(project)
        checkEquals(listOf("a", "b"), helper.allNames())
    }

    fun `test by name`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals(root.resolve("a").toString(), helper.projectDirPathByName("a")!!)
        checkEquals(root.resolve("b").toString(), helper.projectDirPathByName("b")!!)
        assertNull(helper.projectDirPathByName("gnu"))
        assertNull(helper.projectDirPathByName("without-tests"))
    }

    fun `test by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals("a", helper.nameByProjectDirPath(root.resolve("a").toString())!!)
        checkEquals("b", helper.nameByProjectDirPath(root.resolve("b").toString())!!)
        assertNull(helper.nameByProjectDirPath(root.resolve("Toto").toString()))
    }

    fun `test elm project by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals("a",
                helper.elmProjectByProjectDirPath(root.resolve("a").toString())
                        ?.presentableName!!
        )
        checkEquals("b",
                helper.elmProjectByProjectDirPath(root.resolve("b").toString())
                        ?.presentableName!!
        )
        assertNull(helper.elmProjectByProjectDirPath(root.resolve("Toto").toString()))
    }

    private fun testProject(): TestProject {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm")
                }
                dir("tests") {
                }
            }
            dir("without-tests") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm")
                }
            }
            dir("b") {
                project("elm.json", elmJson)
                dir("src") {
                    elm("Main.elm")
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

    val elmJson = """{
        "type": "application",
        "source-directories": [ "src" ],
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
    """
}
