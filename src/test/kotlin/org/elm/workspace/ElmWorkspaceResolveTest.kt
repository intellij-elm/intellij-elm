package org.elm.workspace

import org.elm.TestClientLocation
import org.elm.fileTree
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.stubs.index.ElmModulesIndex
import org.elm.openapiext.pathAsPath

class ElmWorkspaceResolveTest : ElmWorkspaceTestBase() {


    fun `test resolves modules found within source-directories`() {
        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
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
            """.trimIndent())
            dir("src") {
                elm("Main.elm", """
                    import Foo
                           --^
                """.trimIndent())

                elm("Foo.elm", """
                    module Foo exposing (..)
                """.trimIndent())

            }
        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm")
    }


    fun `test resolves modules using double-dot src dir`() {
        val testProject = fileTree {
            dir("example") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [
                            "src",
                            "../src"
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
                    """.trimIndent())
                dir("src") {
                    elm("Main.elm", """
                    module Main exposing (..)
                    import FooBar
                           --^
                    """.trimIndent())
                }
            }
            project("elm.json", """
                {
                    "type": "application",
                    "source-directories": [
                        "src"
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
                """.trimIndent())
            dir("src") {
                elm("FooBar.elm", """
                    module FooBar exposing (x)
                    x = 42
                    """.trimIndent())
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            // it's critical for the test that they are attached in this order, sequentially
            asyncAttachElmProject(rootPath.resolve("example/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("elm.json")).get()
        }

        val elmProjA = project.elmWorkspace.allProjects[0]
        val elmProjB = project.elmWorkspace.allProjects[1]

        val debug = false
        if (debug) {
            println("A is $elmProjA, ${elmProjA.manifestPath}")
            println("B is $elmProjB, ${elmProjB.manifestPath}")
            val moduleDeclsForA = ElmModulesIndex.getAll(TestClientLocation(project, elmProjA))
            val moduleDeclsForB = ElmModulesIndex.getAll(TestClientLocation(project, elmProjB))
            println("module decls for A")
            moduleDeclsForA.forEach { println(it.elmFile.virtualFile.path) }
            println("\n\n")
            println("module decls for B")
            moduleDeclsForB.forEach { println(it.elmFile.virtualFile.path) }
        }

        testProject.run {
            checkReferenceIsResolved<ElmImportClause>("example/src/Main.elm")
        }
    }


    fun `test does not resolve modules outside of source-directories`() {
        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
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
            """.trimIndent())
            dir("src") {
                elm("Main.elm", """
                    import Foo
                           --^
                    """.trimIndent())
            }
            dir("other") {
                elm("Foo.elm", """
                    module Foo exposing (..)
                    """.trimIndent())

            }
        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm", shouldNotResolve = true)
    }


    fun `test resolves modules provided by packages which are direct dependencies`() {

        ensureElmStdlibInstalled(FullElmStdlibVariant)

        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/time": "1.0.0"
                    },
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent())
            dir("src") {
                elm("Main.elm", """
                    import Time
                           --^
                """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm", toPackage = "elm/time 1.0.0")
    }


    fun `test does not resolve unexposed modules provided by direct packages dependencies`() {

        ensureElmStdlibInstalled(CustomElmStdlibVariant(mapOf("elm/parser" to Version(1, 0, 0))))

        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/parser": "1.0.0"
                    },
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent())
            dir("src") {
                elm("Main.elm", """
                    import Parser.Advanced -- this module is part of elm/parser, but it's not exposed
                           --^
                """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm", shouldNotResolve = true)
    }


    fun `test resolves modules to correct version`() {

        ensureElmStdlibInstalled(CustomElmStdlibVariant(mapOf("elm/parser" to Version(1, 0, 0))))
        ensureElmStdlibInstalled(CustomElmStdlibVariant(mapOf("elm/parser" to Version(1, 1, 0))))

        // note that one depends on `elm/parser` 1.0.0 and the other on 1.1.0
        val testProject = fileTree {
            dir("a") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [
                            "src"
                        ],
                        "elm-version": "0.19.1",
                        "dependencies": {
                            "direct": {
                                "elm/parser": "1.0.0"
                            },
                            "indirect": {}
                        },
                        "test-dependencies": {
                            "direct": {},
                            "indirect": {}
                        }
                    }
                    """.trimIndent())
                dir("src") {
                    elm("Main.elm", """
                    import Parser
                           --^
                    """.trimIndent())
                }
            }
            dir("b") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [
                            "src"
                        ],
                        "elm-version": "0.19.1",
                        "dependencies": {
                            "direct": {
                                "elm/parser": "1.1.0"
                            },
                            "indirect": {}
                        },
                        "test-dependencies": {
                            "direct": {},
                            "indirect": {}
                        }
                    }
                    """.trimIndent())
                dir("src") {
                    elm("Main.elm", """
                    import Parser
                           --^
                    """.trimIndent())
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val rootPath = testProject.root.pathAsPath
        project.elmWorkspace.apply {
            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
            asyncAttachElmProject(rootPath.resolve("b/elm.json")).get()
        }

        val elmProjA = project.elmWorkspace.allProjects.find { it.presentableName == "a" }!!
        val elmProjB = project.elmWorkspace.allProjects.find { it.presentableName == "b" }!!

        val debug = false
        if (debug) {
            println("A is $elmProjA, ${elmProjA.manifestPath}")
            println("B is $elmProjB, ${elmProjB.manifestPath}")
            val moduleDeclsForA = ElmModulesIndex.getAll(TestClientLocation(project, elmProjA))
            val moduleDeclsForB = ElmModulesIndex.getAll(TestClientLocation(project, elmProjB))
            println("module decls for A")
            moduleDeclsForA.forEach { println(it.elmFile.virtualFile.path) }
            println("\n\n")
            println("module decls for B")
            moduleDeclsForB.forEach { println(it.elmFile.virtualFile.path) }
        }

        testProject.run {
            checkReferenceIsResolved<ElmImportClause>("a/src/Main.elm", toPackage = "elm/parser 1.0.0")
            checkReferenceIsResolved<ElmImportClause>("b/src/Main.elm", toPackage = "elm/parser 1.1.0")
        }
    }


    fun `test resolves modules provided by packages which are direct test dependencies`() =
            testTestDependencyResolution(true)


    fun `test resolves modules provided by packages which are direct test dependencies in custom tests folder`() =
            testTestDependencyResolution(false)


    private fun testTestDependencyResolution(useDefaultTestsLocation: Boolean) {

        ensureElmStdlibInstalled(FullElmStdlibVariant)

        val testsFolder = if (useDefaultTestsLocation) "tests" else "custom-tests"

        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {},
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {
                        "elm-explorations/test": "1.0.0"
                    },
                    "indirect": {}
                }
            }
            """.trimIndent())
            if (!useDefaultTestsLocation)
                file("elm.intellij.json", """{"test-directory": "custom-tests"}""")
            dir(testsFolder) {
                elm("MyTests.elm", """
                    import Test
                           --^
                """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("$testsFolder/MyTests.elm", toPackage = "elm-explorations/test 1.0.0")
    }


    fun `test does not resolve test dependencies when outside of the 'tests' directory`() {

        ensureElmStdlibInstalled(FullElmStdlibVariant)

        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {},
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {
                        "elm-explorations/test": "1.0.0"
                    },
                    "indirect": {}
                }
            }
            """.trimIndent())
            dir("src") {
                elm("Main.elm", """
                    import Test
                           --^
                """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm", shouldNotResolve = true)
    }


    // See https://github.com/klazuka/intellij-elm/issues/189
    fun `test resolves companion modules inside the default tests directory`() =
            testTestCompanionModulesResolution(true)


    fun `test resolves companion modules inside a custom tests directory`() =
            testTestCompanionModulesResolution(false)


    private fun testTestCompanionModulesResolution(useDefaultTestsLocation: Boolean) {

        ensureElmStdlibInstalled(FullElmStdlibVariant)

        val testsFolder = if (useDefaultTestsLocation) "tests" else "custom-tests"

        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {},
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {
                        "elm-explorations/test": "1.0.0"
                    },
                    "indirect": {}
                }
            }
            """.trimIndent())
            if (!useDefaultTestsLocation)
                file("elm.intellij.json", """{"test-directory": "custom-tests"}""")
            dir(testsFolder) {
                elm("MyTests.elm", """
                    import Helper
                           --^
                """.trimIndent())
                elm("Helper.elm", """
                    module Helper exposing (..)
                """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("$testsFolder/MyTests.elm")
    }


    // TODO [kl] re-enable once a distinction is made between direct and indirect deps
//    fun `test does not resolve modules which are not direct dependencies`() {
//        buildProject {
//            project("elm.json", """
//            {
//                "type": "application",
//                "source-directories": [
//                    "src"
//                ],
//                "elm-version": "0.19.1",
//                "dependencies": {
//                    "direct": {},
//                    "indirect": {
//                        "elm/time": "1.0.0"
//                    }
//                },
//                "test-dependencies": {
//                    "direct": {},
//                    "indirect": {}
//                }
//            }
//            """.trimIndent())
//            dir("src") {
//                elm("Main.elm", """
//                    import Time
//                           --^
//                """.trimIndent())
//            }
//        }.checkReferenceIsResolved<ElmImportClause>("src/Main.elm", shouldNotResolve = true)
//    }


}
