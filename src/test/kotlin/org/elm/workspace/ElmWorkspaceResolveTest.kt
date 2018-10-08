package org.elm.workspace

import org.elm.FileTreeBuilder
import org.elm.fileTree
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.stubs.index.ElmModules
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

    fun `test resolves modules found within source-directories in parent`() {
        // this test ensures that we properly handle source-dirs like "../vendor"
        buildProject {
            dir("app") {
                project("elm.json", """
                    {
                        "type": "application",
                        "source-directories": [
                            "src",
                            "../vendor"
                        ],
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
                    """.trimIndent())
                dir("src") {
                    elm("Main.elm", """
                    import Foo
                           --^
                    """.trimIndent())
                }
            }
            dir("vendor") {
                elm("Foo.elm", """
                    module Foo exposing (..)
                    """.trimIndent())
            }
        }.checkReferenceIsResolved<ElmImportClause>("app/src/Main.elm")
    }


    fun `test does not resolve modules outside of source-directories`() {
        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
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
                "elm-version": "0.19.0",
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

    // TODO [kl] re-enable once I have a way to install multiple versions of the same package via tests
//    fun `test resolves modules to correct version`() {
//        // note that one depends on `elm-explorations/test` 1.0.0 and the other on 1.1.0
//        val testProject = fileTree {
//            dir("a") {
//                project("elm.json", """
//                    {
//                        "type": "application",
//                        "source-directories": [
//                            "src"
//                        ],
//                        "elm-version": "0.19.0",
//                        "dependencies": {
//                            "direct": {
//                                "elm-explorations/test": "1.0.0"
//                            },
//                            "indirect": {}
//                        },
//                        "test-dependencies": {
//                            "direct": {},
//                            "indirect": {}
//                        }
//                    }
//                    """.trimIndent())
//                dir("src") {
//                    elm("Main.elm", """
//                    import Expect
//                           --^
//                    """.trimIndent())
//                }
//            }
//            dir("b") {
//                project("elm.json", """
//                    {
//                        "type": "application",
//                        "source-directories": [
//                            "src"
//                        ],
//                        "elm-version": "0.19.0",
//                        "dependencies": {
//                            "direct": {
//                                "elm-explorations/test": "1.1.0"
//                            },
//                            "indirect": {}
//                        },
//                        "test-dependencies": {
//                            "direct": {},
//                            "indirect": {}
//                        }
//                    }
//                    """.trimIndent())
//                dir("src") {
//                    elm("Main.elm", """
//                    import Expect
//                           --^
//                    """.trimIndent())
//                }
//            }
//        }.create(project, elmWorkspaceDirectory)
//
//        val rootPath = testProject.root.pathAsPath
//        project.elmWorkspace.apply {
//            asyncAttachElmProject(rootPath.resolve("a/elm.json")).get()
//            asyncAttachElmProject(rootPath.resolve("b/elm.json")).get()
//        }
//
//        val elmProjA = project.elmWorkspace.allProjects.find { it.presentableName == "a" }!!
//        val elmProjB = project.elmWorkspace.allProjects.find { it.presentableName == "b" }!!
//
//        val debug = false
//        if (debug) {
//            println("A is $elmProjA, ${elmProjA.manifestPath}")
//            println("B is $elmProjB, ${elmProjB.manifestPath}")
//
//            val moduleDeclsForA = ElmModules.getAll(project, elmProjA)
//            val moduleDeclsForB = ElmModules.getAll(project, elmProjB)
//
//            println("module decls for A (expect 1.0.0)")
//            moduleDeclsForA.forEach { println(it.elmFile.virtualFile.path) }
//
//            println("\n\n")
//
//            println("module decls for B (expect 1.1.0)")
//            moduleDeclsForB.forEach { println(it.elmFile.virtualFile.path) }
//        }
//
//        testProject.run {
//            checkReferenceIsResolved<ElmImportClause>("a/src/Main.elm", toPackage = "elm-explorations/test 1.0.0")
//            checkReferenceIsResolved<ElmImportClause>("b/src/Main.elm", toPackage = "elm-explorations/test 1.1.0")
//        }
//    }


    // TODO [kl] re-enable once a distinction is made between direct and indirect deps
//    fun `test does not resolve modules which are not direct dependencies`() {
//        buildProject {
//            project("elm.json", """
//            {
//                "type": "application",
//                "source-directories": [
//                    "src"
//                ],
//                "elm-version": "0.19.0",
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


    fun buildProject(builder: FileTreeBuilder.() -> Unit) =
            fileTree(builder).asyncCreateWithAutoDiscover().get()
}