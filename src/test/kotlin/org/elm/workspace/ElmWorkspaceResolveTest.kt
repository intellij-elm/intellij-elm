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

    fun buildProject(builder: FileTreeBuilder.() -> Unit) =
            fileTree(builder).asyncCreateWithAutoDiscover().get()
}