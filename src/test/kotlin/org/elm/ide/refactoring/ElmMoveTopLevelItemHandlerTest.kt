package org.elm.ide.refactoring

import com.intellij.psi.PsiFile
import org.elm.TestProject
import org.elm.ide.refactoring.move.ElmMoveTopLevelItemsProcessor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.workspace.ElmWorkspaceTestBase

class ElmMoveTopLevelItemHandlerTest : ElmWorkspaceTestBase() {

    fun `test value declaration`() {
        makeTestProjectFixture()

        val sourceFile: PsiFile = myFixture.configureByFile("src/Foo/Baz.elm")
        val targetFile: ElmFile = myFixture.configureByFile("src/Bar/Buff.elm") as ElmFile

        ElmMoveTopLevelItemsProcessor(
            myFixture.project,
            arrayOf(sourceFile.descendantsOfType<ElmValueDeclaration>().first()),
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

    fun `test removes and adds exposed values`() {
        makeTestProjectFixture("SPECIFIED_EXPOSE")

        val sourceFile: PsiFile = myFixture.configureByFile("src/Foo/Baz.elm")
        val targetFile: ElmFile = myFixture.configureByFile("src/Bar/Buff.elm") as ElmFile

        ElmMoveTopLevelItemsProcessor(
            myFixture.project,
            arrayOf(sourceFile.descendantsOfType<ElmValueDeclaration>().first()),
            targetFile,
            true
        ).run()

        myFixture.checkResult(
            "src/Foo/Baz.elm", """
            module Foo.Baz exposing (placeholderValue2)
            
            placeholderValue2 = 0
        """.trimIndent(), true
        )

        myFixture.checkResult(
            "src/Bar/Buff.elm", """
            module Bar.Buff exposing (placeholderValue3, placeholderValue)
            
            placeholderValue3 = 0
            placeholderValue = 0
        """.trimIndent(), true
        )
    }

    fun `test adds import statements for dependants`() {

    }

    fun `test adds import statements for aliased dependants`() {

    }

    fun `test adds import statements for dependencies`() {

    }

    fun `test adds import statements for aliased dependencies`() {

    }

    private fun makeTestProjectFixture(type: String = ""): TestProject =
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
                            "elm/core": "1.0.0",
                            "elm/json": "1.0.0"                        
                        }, 
                        "indirect": {}
                    },
                    "test-dependencies": { "direct": {}, "indirect": {} }
                }
                """.trimIndent())

            if (type == "") {
                dir("src") {
                    elm("Main.elm")
                    dir("Foo") {
                        elm(
                            "Baz.elm", """
                        module Foo.Baz exposing (..)
                        placeholderValue = 0
                    """.trimIndent()
                        )
                    }
                    dir("Bar") {
                        elm(
                            "Buff.elm", """
                        module Bar.Buff exposing (..)
                    """.trimIndent()
                        )
                    }
                }
            } else if (type == "SPECIFIED_EXPOSE") {
                dir("src") {
                    elm("Main.elm")
                    dir("Foo") {
                        elm(
                            "Baz.elm", """
                        module Foo.Baz exposing (placeholderValue, placeholderValue2)
                        placeholderValue = 0
                        placeholderValue2 = 0
                    """.trimIndent()
                        )
                    }
                    dir("Bar") {
                        elm(
                            "Buff.elm", """
                        module Bar.Buff exposing (placeholderValue3)
                        
                        placeholderValue3 = 0
                    """.trimIndent()
                        )
                    }
                }
            }
        }
}

