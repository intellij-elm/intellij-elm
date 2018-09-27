package org.elm.workspace

import org.elm.FileTreeBuilder
import org.elm.fileTree
import org.elm.lang.core.psi.elements.ElmImportClause

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


    fun buildProject(builder: FileTreeBuilder.() -> Unit) =
            fileTree(builder).asyncCreateWithAutoDiscover().get()
}