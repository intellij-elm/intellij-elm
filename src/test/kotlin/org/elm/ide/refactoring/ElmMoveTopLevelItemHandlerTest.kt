package org.elm.ide.refactoring

import com.intellij.psi.PsiFile
import org.elm.TestProject
import org.elm.ide.refactoring.move.ElmMoveTopLevelItemsProcessor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.workspace.ElmWorkspaceTestBase

class ElmMoveTopLevelItemHandlerTest : ElmWorkspaceTestBase() {

    fun `test basic value declaration movement`() {
        makeTestProjectFixture()

        val sourceFile: PsiFile = myFixture.configureByFile("src/Foo/Baz.elm")
        val targetFile: ElmFile = myFixture.configureByFile("src/Bar/Buff.elm") as ElmFile

        ElmMoveTopLevelItemsProcessor(
            myFixture.project,
            sourceFile.descendantsOfType<ElmValueDeclaration>().toTypedArray(),
            targetFile,
            true
        ).run()

        myFixture.checkResult(
            "src/Foo/Baz.elm", """
            module Foo.Baz exposing (..)
            
        """.trimIndent(), true
        )

        myFixture.checkResult(
            "src/Bar/Buff.elm", """
            module Bar.Buff exposing (..)
            placeholderValue = 0
        """.trimIndent(), true
        )
    }

    private fun makeTestProjectFixture(): TestProject =
        buildProject {
            project("elm.json", """
                {
                    "type": "application",
                    "source-directories": [
                        "src",
                        "vendor/elm-foo",
                        "./foo1"
                    ],
                    "elm-version": "0.19.1",
                    "dependencies": {
                        "direct": {
                            "elm/core": "1.0.0",
                            "elm/json": "1.0.0"                        
                        }, 
                        "indirect": {}
                    },
                    "test-dependencies": { "direct": {}, "indirect": {} }
                }
                """.trimIndent())
            dir("src") {
                elm("Main.elm")
                dir("Foo") {
                    elm("Baz.elm", """
                        module Foo.Baz exposing (..)
                        placeholderValue = 0
                    """.trimIndent())
                }
                dir("Bar") {
                    elm("Buff.elm", """
                        module Bar.Buff exposing (..)
                    """.trimIndent())
                }
            }
            dir("vendor") {
                dir("elm-foo") {
                    dir("Internals") {}
                }
            }
            dir("foo1") {
                dir("Foo1") {}
            }
            dir("tests") {
                dir("Legacy") {}
            }
            dir("outside") {}
        }
}

