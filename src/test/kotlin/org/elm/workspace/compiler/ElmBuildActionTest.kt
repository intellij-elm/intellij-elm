package org.elm.workspace.compiler

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase
import org.intellij.lang.annotations.Language
import java.nio.file.Path

class ElmBuildActionTest : ElmWorkspaceTestBase() {


    fun `test build Elm application project`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", """
                    import Html
                    main = Html.text "hi"{-caret-}
                """.trimIndent())
            }
        }.fileWithCaret
        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        doTest(file, expectedNumErrors = 0)
    }


    fun `test build Elm application project with an error`() {
        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", """
                    import Html
                    foo = bogus{-caret-}
                    main = Html.text "hi"
                """.trimIndent())
            }
        }.fileWithCaret
        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        doTest(file, expectedNumErrors = 1)
    }


    private fun doTest(file: VirtualFile, expectedNumErrors: Int) {
        var succeeded = false
        with(project.messageBus.connect(testRootDisposable)) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>) {
                    succeeded = messages.size == expectedNumErrors
                }
            })
        }

        val (action, event) = makeTestAction(file)
        check(event.presentation.isEnabledAndVisible) {
            "The build action should be enabled in this context"
        }
        action.actionPerformed(event)
        TestCase.assertTrue(succeeded)
    }


    private fun makeTestAction(file: VirtualFile): Pair<ElmBuildAction, TestActionEvent> {
        val dataContext = MapDataContext(mapOf(
                CommonDataKeys.PROJECT to project,
                CommonDataKeys.VIRTUAL_FILE to file
        ))
        val action = ElmBuildAction()
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
                "direct": {
                    "elm/core": "1.0.0",
                    "elm/html": "1.0.0",
                    "elm/json": "1.0.0",
                    "elm/time": "1.0.0"
                },
                "indirect": {
                    "elm/virtual-dom": "1.0.2"
                }
            },
            "test-dependencies": {
                "direct": {},
                "indirect": {}
            }
        }
        """.trimIndent()
