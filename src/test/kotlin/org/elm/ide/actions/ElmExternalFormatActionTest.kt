package org.elm.ide.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase
import org.intellij.lang.annotations.Language

class ElmExternalFormatActionTest : ElmWorkspaceTestBase() {

    override fun runTest() {
        if (toolchain?.elmFormat == null) {
            // TODO in the future maybe we should install elm-format in the CI build environment
            System.err.println("SKIP $name: elm-format not found")
            return
        }
        super.runTest()
    }


    fun `test elm-format action with elm 19`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        reformat(file)
        val expected = """
                    module Main exposing (f)


                    f x =
                        x

                """.trimIndent()
        TestCase.assertEquals(expected, document.text)
    }


    // TODO [drop 0.18] remove this test
    fun `test elm-format action with elm 18`() {
        val fileWithCaret = buildProject {
            project("elm-package.json", manifestElm18)
            dir("src") {
                elm("Main.elm", """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
            dir("elm-stuff") {
                file("exact-dependencies.json", "{}")
            }
        }.fileWithCaret

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        reformat(file)
        val expected = """
                    module Main exposing (f)


                    f x =
                        x

                """.trimIndent()
        TestCase.assertEquals(expected, document.text)
    }

    fun `test elm-format action shouldn't be active on non-elm files`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19.trimIndent())
            dir("src") {
                elm("Main.scala", """
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val (_, e) = makeTestAction(file)
        check(!e.presentation.isEnabledAndVisible) {
            "The elm-format action shouldn't be enabled in this context"
        }
    }

    private fun reformat(file: VirtualFile) {
        val (action, event) = makeTestAction(file)
        action.beforeActionPerformedUpdate(event)
        check(event.presentation.isEnabledAndVisible) {
            "The elm-format action should be enabled in this context"
        }
        action.actionPerformed(event)
    }

    private fun makeTestAction(file: VirtualFile): Pair<ElmExternalFormatAction, TestActionEvent> {
        val dataContext = MapDataContext(mapOf(
                CommonDataKeys.PROJECT to project,
                CommonDataKeys.VIRTUAL_FILE to file
        ))
        val action = ElmExternalFormatAction()
        val event = TestActionEvent(dataContext, action)
        action.beforeActionPerformedUpdate(event)
        return Pair(action, event)
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