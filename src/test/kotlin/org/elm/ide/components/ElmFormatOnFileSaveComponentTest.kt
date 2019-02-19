package org.elm.ide.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace
import org.intellij.lang.annotations.Language

class ElmFormatOnFileSaveComponentTest : ElmWorkspaceTestBase() {

    override fun runTest() {
        if (toolchain?.elmFormat == null) {
            // TODO in the future maybe we should install elm-format in the CI build environment
            System.err.println("SKIP $name: elm-format not found")
            return
        }
        super.runTest()
    }

    val unformatted = """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent()

    val expectedFormatted = """
                    module Main exposing (f)


                    f x =
                        x

                """.trimIndent()


    // TODO [drop 0.18] remove this test
    fun `test ElmFormatOnFileSaveComponent should work with elm 18`() {

        val fileWithCaret: String = buildProject {
            project("elm-package.json", manifestElm18)
            dir("src") {
                elm("Main.elm", unformatted)
            }
            dir("elm-stuff") {
                file("exact-dependencies.json", "{}")
            }
        }.fileWithCaret

        testCorrectFormatting(fileWithCaret, unformatted, expectedFormatted)

    }

    fun `test ElmFormatOnFileSaveComponent should work with elm 19`() {

        val fileWithCaret: String = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", unformatted)
            }
        }.fileWithCaret

        testCorrectFormatting(fileWithCaret, unformatted, expectedFormatted)
    }

    fun `test ElmFormatOnFileSaveComponent should not add to the undo stack`() {

        val fileWithCaret: String = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", unformatted)
            }
        }.fileWithCaret

        testCorrectFormatting(fileWithCaret, unformatted, expectedFormatted)

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)

        val undoManager = UndoManager.getInstance(project)
        TestCase.assertFalse(undoManager.isUndoAvailable(fileEditor))
    }

    fun `test ElmFormatOnFileSaveComponent should not touch a file with the wrong ending like 'scala'`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.scala", """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        testCorrectFormatting(
                fileWithCaret,
                unformatted,
                expected = unformatted.replace("{-caret-}", "")
        )
    }

    fun `test ElmFormatOnFileSaveComponent should not touch a file if the save-hook is deactivated`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        testCorrectFormatting(
                fileWithCaret,
                unformatted,
                expected = unformatted.replace("{-caret-}", ""),
                activateOnSaveHook = false
        )
    }

    fun `test ElmFormatOnFileSaveComponent should not touch a file syntax errors`() {
        val brokenElmCode = """
                    m0dule Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent()
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.scala", brokenElmCode)
            }
        }.fileWithCaret

        testCorrectFormatting(
                fileWithCaret,
                brokenElmCode,
                expected = brokenElmCode.replace("{-caret-}", "")
        )
    }

    private fun testCorrectFormatting(fileWithCaret: String, unformatted: String, expected: String, activateOnSaveHook: Boolean = true) {

        project.elmWorkspace.useToolchain(toolchain?.copy(isElmFormatOnSaveEnabled = activateOnSaveHook))

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val fileDocumentManager = FileDocumentManager.getInstance()
        val document = fileDocumentManager.getDocument(file)!!

        // set text to mark file as unsaved
        // (can't be the unmodified document.text since this won't trigger the beforeDocumentSaving callback)
        val newContent = unformatted.replace("{-caret-}", "")
        ApplicationManager.getApplication().runWriteAction {
            document.setText(newContent)
        }

        fileDocumentManager.saveDocument(document)

        TestCase.assertEquals(expected, document.text)
    }
}


@Language("JSON")
private val manifestElm19 = """
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
        """.trimIndent()


// TODO [drop 0.18]
@Language("JSON")
private val manifestElm18 = """
        {
          "elm-version": "0.18.0 <= v < 0.19.0",
          "version": "1.0.0",
          "summary": "",
          "repository": "",
          "license": "",
          "source-directories": [ "src" ],
          "exposed-modules": [],
          "dependencies": {}
        }
        """.trimIndent()