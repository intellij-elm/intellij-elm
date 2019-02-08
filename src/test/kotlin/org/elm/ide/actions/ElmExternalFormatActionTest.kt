package org.elm.ide.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase

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
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        reformat(PsiManager.getInstance(project).findFile(file)!!)
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
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.18.0",
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
                    module Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent())
            }
        }.fileWithCaret

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        reformat(PsiManager.getInstance(project).findFile(file)!!)
        val expected = """
                    module Main exposing (f)


                    f x =
                        x

                """.trimIndent()
        TestCase.assertEquals(expected, document.text)
    }

    private fun reformat(file: PsiFile) {
        val dataContext = MapDataContext(mapOf(
                CommonDataKeys.PROJECT to project,
                CommonDataKeys.PSI_FILE to file
        ))
        val action = ElmExternalFormatAction()
        val e = TestActionEvent(dataContext, action)
        action.beforeActionPerformedUpdate(e)
        check(e.presentation.isEnabledAndVisible) {
            "The elm-format action should be enabled in this context"
        }
        action.actionPerformed(e)
    }
}