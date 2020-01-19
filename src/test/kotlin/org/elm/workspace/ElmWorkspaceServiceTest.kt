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
                project("elm.json", BASIC_APPLICATION_MANIFEST)
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
        ensureElmStdlibInstalled(FullElmStdlibVariant)
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [ "src", "vendor" ],
                        "elm-version": "0.19.1",
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

        checkEquals(Version(0, 19, 1), elmProject.elmVersion)
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
        ensureElmStdlibInstalled(FullElmStdlibVariant)
        val testProject = fileTree {
            project("elm.json", """
                    {
                        "type": "package",
                        "name": "elm/json",
                        "summary": "Encode and decode JSON values",
                        "license": "BSD-3-Clause",
                        "version": "1.2.3",
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

        checkEquals(Version(1, 2, 3), elmProject.version)

        // The source directory for Elm 0.19 packages is implicitly "src". It cannot be changed.
        checkEquals(setOf(Paths.get("src")), elmProject.sourceDirectories.toSet())

        checkEquals(setOf("elm/core" to Version(1, 0, 0)),
                elmProject.dependencies.map { it.name to it.version }.toSet())

        checkEquals(setOf("elm-explorations/test" to Version(1, 0, 0)),
                elmProject.testDependencies.map { it.name to it.version }.toSet())

        checkEquals(setOf("Json.Decode", "Json.Encode"),
                elmProject.exposedModules.toSet())
    }


    fun `test can attach multiple Elm projects`() {
        val testProject = fileTree {
            dir("a") {
                project("elm.json", BASIC_APPLICATION_MANIFEST)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
            dir("b") {
                project("elm.json", BASIC_APPLICATION_MANIFEST)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("b/elm.json")).get()
        }

        fun checkFile(relativePath: String, projectName: String?) {
            val vFile = testProject.root.findFileByRelativePath(relativePath)!!
            val project = workspace.findProjectForFile(vFile)
            if (project?.presentableName != projectName) {
                error("Expected $projectName, found $project for $relativePath")
            }
        }

        checkFile("a/src/Main.elm", "a")
        checkFile("b/src/Main.elm", "b")
    }


    // BEGIN ELM 0.18 LEGACY

    // TODO [drop 0.18] remove this test
    fun `test can attach legacy Elm 18 application json files`() {
        // these Elm 0.18 tests are gross, but we will be deleting it "soon" so who cares
        val testProject = fileTree {
            project("elm-package.json", """
                {
                  "version": "1.0.0",
                  "summary": "blah",
                  "repository": "https://github.com/user/project.git",
                  "license": "BSD3",
                  "source-directories": [
                    "./src"
                  ],
                  "exposed-modules": [],
                  "dependencies": {
                    "elm-lang/core": "5.0.0 <= v < 6.0.0"
                  },
                  "elm-version": "0.18.0 <= v < 0.19.0"
                }
                """)
            dir("elm-stuff") {
                file("exact-dependencies.json", """
                    {
                        "elm-lang/core": "5.1.1"
                    }
                """.trimIndent())
                dir("packages") {
                    dir("elm-lang") {
                        dir("core") {
                            dir("5.1.1") {
                                project("elm-package.json", """
                                    {
                                        "version": "5.1.1",
                                        "summary": "Elm's standard libraries",
                                        "repository": "http://github.com/elm-lang/core.git",
                                        "license": "BSD3",
                                        "source-directories": [
                                            "src"
                                        ],
                                        "exposed-modules": [
                                            "Array",
                                            "Basics",
                                            "Bitwise",
                                            "Char",
                                            "Color",
                                            "Date",
                                            "Debug",
                                            "Dict",
                                            "Json.Decode",
                                            "Json.Encode",
                                            "List",
                                            "Maybe",
                                            "Platform",
                                            "Platform.Cmd",
                                            "Platform.Sub",
                                            "Process",
                                            "Random",
                                            "Regex",
                                            "Result",
                                            "Set",
                                            "String",
                                            "Task",
                                            "Time",
                                            "Tuple"
                                        ],
                                        "native-modules": true,
                                        "dependencies": {},
                                        "elm-version": "0.18.0 <= v < 0.19.0"
                                    }
                                """.trimIndent())
                                dir("src") {}
                            }
                        }
                    }
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("elm-package.json")).get()
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

        checkEquals(Version(0, 18, 0), elmProject.elmVersion)

        checkEquals(setOf("elm-lang/core" to Version(5, 1, 1)),
                elmProject.dependencies.map { it.name to it.version }.toSet())
    }

    // TODO [drop 0.18] remove this test
    fun `test can attach legacy Elm 18 package json files`() {
        val testProject = fileTree {
            project("elm-package.json", """
                {
                  "version": "1.2.3",
                  "summary": "blah",
                  "repository": "https://github.com/user/project.git",
                  "license": "BSD3",
                  "source-directories": [
                    "."
                  ],
                  "exposed-modules": ["Foo", "Bar"],
                  "dependencies": {
                    "elm-lang/core": "5.0.0 <= v < 6.0.0"
                  },
                  "elm-version": "0.18.0 <= v < 0.19.0"
                }
                """)
            dir("elm-stuff") {
                file("exact-dependencies.json", """
                    {
                        "elm-lang/core": "5.1.1"
                    }
                """.trimIndent())
                dir("packages") {
                    dir("elm-lang") {
                        dir("core") {
                            dir("5.1.1") {
                                project("elm-package.json", """
                                    {
                                        "version": "5.1.1",
                                        "summary": "Elm's standard libraries",
                                        "repository": "http://github.com/elm-lang/core.git",
                                        "license": "BSD3",
                                        "source-directories": [
                                            "src"
                                        ],
                                        "exposed-modules": [
                                            "Array",
                                            "Basics",
                                            "Bitwise",
                                            "Char",
                                            "Color",
                                            "Date",
                                            "Debug",
                                            "Dict",
                                            "Json.Decode",
                                            "Json.Encode",
                                            "List",
                                            "Maybe",
                                            "Platform",
                                            "Platform.Cmd",
                                            "Platform.Sub",
                                            "Process",
                                            "Random",
                                            "Regex",
                                            "Result",
                                            "Set",
                                            "String",
                                            "Task",
                                            "Time",
                                            "Tuple"
                                        ],
                                        "native-modules": true,
                                        "dependencies": {},
                                        "elm-version": "0.18.0 <= v < 0.19.0"
                                    }
                                """.trimIndent())
                                dir("src") {}
                            }
                        }
                    }
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("elm-package.json")).get()
        }

        val elmProject = workspace.allProjects.firstOrNull()
        if (elmProject == null) {
            TestCase.fail("failed to find an Elm project")
            return
        }

        if (elmProject !is ElmPackageProject) {
            TestCase.fail("expected an Elm application project, got $elmProject")
            return
        }

        checkEquals(makeConstraint(Version(0, 18, 0), Version(0, 19, 0)), elmProject.elmVersion)

        checkEquals(Version(1, 2, 3), elmProject.version)

        // Elm 0.18 packages can specify a source directory (unlike Elm 0.19)
        checkEquals(setOf(Paths.get(".")), elmProject.sourceDirectories.toSet())

        checkEquals(setOf("elm-lang/core" to Version(5, 1, 1)),
                elmProject.dependencies.map { it.name to it.version }.toSet())

        checkEquals(listOf("Foo", "Bar"), elmProject.exposedModules)
    }

    // END ELM 0.18 LEGACY


    fun `test auto discover Elm project at root level`() {
        val testProject = fileTree {
            project("elm.json", BASIC_APPLICATION_MANIFEST)
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

    fun `test persistence roundtrip`() {
        // setup real files on disk
        val testProject = fileTree {
            dir("a") {
                project("elm.json", BASIC_APPLICATION_MANIFEST)
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
              <settings elmCompilerPath="${toolchain.elmCompilerPath}" elmFormatPath="${toolchain.elmFormatPath}" elmTestPath="${toolchain.elmTestPath}" isElmFormatOnSaveEnabled="true" />
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


    // START OF TESTS RELATED TO CUSTOM MANIFEST (elm.intellij.json)

    fun `test uses default tests location when no custom manifest exists (application project)`() =
            testCustomManifest(null, "tests", false)

    fun `test uses custom tests location when custom manifest exists`() =
            testCustomManifest(getCustomManifest("custom-tests"), "custom-tests", true)

    fun `test uses default tests location when custom manifest specifies no custom location`() =
            testCustomManifest("{}", "tests", false)

    fun `test uses default tests location when custom manifest specifies default location`() =
            testCustomManifest(getCustomManifest("tests"), "tests", false)

    fun `test handles nested custom tests location`() =
            testCustomManifest(getCustomManifest("custom/tests/location"), "custom/tests/location", true)

    fun `test normalizes custom tests location`() =
            testCustomManifest(getCustomManifest("./custom/tests/location/."), "custom/tests/location", true)

    fun `test uses default tests location when no custom manifest exists (package project)`() =
            testCustomManifest(null, "tests", expectedIsCustomTestsDir = false, isApplicationProject = false)

    fun `test ignores custom tests location for packages`() =
            testCustomManifest(getCustomManifest("custom-tests"), "tests", expectedIsCustomTestsDir = false, isApplicationProject = false)

    fun `test auto discover Elm project skips project with bad custom manifest`() {
        fileTree {
            project("elm.json", BASIC_APPLICATION_MANIFEST)
            file("elm.intellij.json", """ { "BOGUS": "INVALID ELM.INTELLIJ.JSON" } """)
            dir("src") {
                elm("Main.elm", "")
            }
        }.create(project, elmWorkspaceDirectory)

        val elmProjects = project.elmWorkspace.asyncDiscoverAndRefresh().get()
        check(elmProjects.isEmpty()) { "Should have found zero Elm projects but found ${elmProjects.size}" }
    }

    /**
     * Executes a test related to the custom manifest (`elm.intellij.json`). Creates a project, adding a custom manifest
     * with the specified `customManifestContent` if not null. Then verifies that the [ElmProject] generated by attaching
     * the manifest has the expected data related to the custom manifest, i.e. information about the tests directory.
     */
    private fun testCustomManifest(
            customManifestContent: String?,
            expectedTestsRelativeDirPath: String,
            expectedIsCustomTestsDir: Boolean,
            isApplicationProject: Boolean = true
    ) {

        val testProject = fileTree {
            dir("a") {
                project("elm.json", if (isApplicationProject) BASIC_APPLICATION_MANIFEST else BASIC_PACKAGE_MANIFEST)
                if (customManifestContent != null)
                    file("elm.intellij.json", customManifestContent)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        val workspace = project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
        }

        val elmProject = workspace.allProjects.firstOrNull() ?: error("No Elm project found")

        checkEquals(expectedTestsRelativeDirPath, elmProject.testsRelativeDirPath)
        checkEquals(expectedIsCustomTestsDir, elmProject.isCustomTestsDir)
        checkEquals(elmProject.projectDirPath.resolve(expectedTestsRelativeDirPath).normalize(), elmProject.testsDirPath)
    }

    // END OF TESTS RELATED TO CUSTOM MANIFEST (elm.intellij.json)
}


private fun makeConstraint(low: Version, high: Version): Constraint {
    return Constraint(
            low = low,
            lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
            highOp = Constraint.Op.LESS_THAN,
            high = high
    )
}

/**
 * A basic minimal `elm.json` file for an application project, with no dependencies.
 * Used in tests which don't really care about the content of the file, just that it's a valid manifest.
 */
private const val BASIC_APPLICATION_MANIFEST = """
{
  "type": "application",
  "source-directories": [ "src" ],
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
"""

/**
 * A basic minimal `elm.json` file for a package project, with no dependencies.
 * Used in tests which don't really care about the content of the file, just that it's a valid manifest.
 */
private const val BASIC_PACKAGE_MANIFEST = """
{
  "type": "package",
  "name": "test",
  "summary": "Test",
  "license": "Test",
  "version": "1.2.3",
  "exposed-modules": [],
  "elm-version": "0.19.0 <= v < 0.20.0",
  "dependencies": { },
  "test-dependencies": { }
}
"""

/**
 * Builds a custom manifest (i.e. `elm.intellij.json`) with the specified test directory.
 */
private fun getCustomManifest(testDir: String): String = """
{
  "test-directory": "$testDir"
}
"""
