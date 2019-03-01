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

        val helper = ElmProjectTestsHelper(project)
        checkEquals(Arrays.asList("a", "b"), helper.allNames().toList())
    }

    fun `test by name`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals(Optional.of(root.resolve("a").toString()), helper.projectDirPathByName("a"))
        checkEquals(Optional.of(root.resolve("b").toString()), helper.projectDirPathByName("b"))
        checkEquals(empty<String>(), helper.projectDirPathByName("gnu"))
        checkEquals(empty<String>(), helper.projectDirPathByName("without-tests"))
    }

    fun `test by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals(Optional.of("a"), helper.nameByProjectDirPath(root.resolve("a").toString()))
        checkEquals(Optional.of("b"), helper.nameByProjectDirPath(root.resolve("b").toString()))
        checkEquals(empty<String>(), helper.nameByProjectDirPath(root.resolve("Toto").toString()))
    }

    fun `test elm project by path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals(
                Optional.of("a"),
                helper.elmProjectByProjectDirPath(root.resolve("a").toString())
                        .map(ElmProject::presentableName)
        )
        checkEquals(Optional.of("b"),
                helper.elmProjectByProjectDirPath(root.resolve("b").toString())
                        .map(ElmProject::presentableName)
        )
        checkEquals(empty<ElmProject>(), helper.elmProjectByProjectDirPath(root.resolve("Toto").toString()))
    }

    fun `test elm18 project`() {
        val testProject = testProject18()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        checkEquals(
                Optional.of(true),
                helper.elmProjectByProjectDirPath(root.resolve("z").toString())
                        .map(ElmProject::isElm18)
        )
        checkEquals(empty<ElmProject>(),
                helper.elmProjectByProjectDirPath(root.resolve("z/tests").toString())
                        .map(ElmProject::isElm18)
        )
    }

    fun `test adjust elm compiler path`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        val compilerPath = root.resolve("bin/elm")
        checkEquals(
                compilerPath,
                helper.adjustElmCompilerProjectDirPath(root.resolve("a").toString(), compilerPath)
        )
    }

    fun `test adjust elm18 compiler path`() {
        val testProject = testProject18()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        val compilerPath = root.resolve("bin/elm")
        checkEquals(
                compilerPath.resolveSibling("elm-make"),
                helper.adjustElmCompilerProjectDirPath(root.resolve("z").toString(), compilerPath)
        )
    }

    fun `test elm folder for testing`() {
        val testProject = testProject()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        val elmProjectA = helper.elmProjectByProjectDirPath(root.resolve("a").toString())
        checkEquals(
                Optional.of(root.resolve("a")),
                elmProjectA.map(ElmProjectTestsHelper::elmFolderForTesting)
        )
    }

    fun `test elm18 folder for testing`() {
        val testProject = testProject18()
        val root = testProject.root.pathAsPath

        val helper = ElmProjectTestsHelper(project)
        val rootZ = root.resolve("z")
        val elmProjectZ = helper.elmProjectByProjectDirPath(rootZ.toString())

        val testFile = testProject.root.findFileByRelativePath("z/tests/Test.elm")!!
        val elmProjectZtest = project.elmWorkspace.findProjectForFile(testFile)!!

        checkEquals(
                Optional.of(rootZ),
                elmProjectZ.map(ElmProjectTestsHelper::elmFolderForTesting)
        )
        checkEquals(
                rootZ,
                ElmProjectTestsHelper.elmFolderForTesting(elmProjectZtest)
        )
        checkEquals(
                root.resolve("z/tests"),
                elmProjectZtest.projectDirPath
        )
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

    val elmJson = """{
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

    private fun testProject18(): TestProject {
        val testProject = fileTree {
            dir("z") {
                project("elm-package.json", elmJson18)
                dir("src") {
                    elm("Main.elm", "")
                }
                dir("elm-stuff") {
                    file("exact-dependencies.json", "{}")
                }
                dir("tests") {
                    project("elm-package.json", elmJson18test)
                    elm("Test.elm", "")
                    dir("elm-stuff") {
                        file("exact-dependencies.json", "{}")
                    }
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("z/elm-package.json")).get()
            asyncAttachElmProject(rootPath.resolve("z/tests/elm-package.json")).get()
        }

        return testProject
    }

    val elmJson18 = """{
        "version": "4.0.0",
        "repository": "https://github.com/user/project.git",
        "license": "BSD-3-Clause",
        "source-directories": [
            "src"
        ],
        "exposed-modules": [],
        "dependencies": {
        },
        "elm-version": "0.18.0 <= v < 0.19.0"
    }
    """

    val elmJson18test = """{
        "version": "4.0.0",
        "repository": "https://github.com/user/project.git",
        "license": "BSD-3-Clause",
        "source-directories": [
            "."
        ],
        "exposed-modules": [],
        "dependencies": {
        },
        "elm-version": "0.18.0 <= v < 0.19.0"
    }
    """
}
