package org.elm.ide.actions

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase
import org.intellij.lang.annotations.Language

class ElmExternalFormatActionTest : ElmWorkspaceTestBase() {

    override fun runTest() {
        if (toolchain.elmFormatCLI == null) {
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

    fun `test elm-format action should not run, when the elm-file has errors and display notification to the user`() {
        val unformatted = """
                    m0dule Main exposing (f)


                    f x = x{-caret-}

                """.trimIndent()

        val fileWithCaret = buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", unformatted)
            }
        }.fileWithCaret

        val notificationRef = connectToBusAndGetNotificationRef()

        val file = myFixture.configureFromTempProjectFile(fileWithCaret).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!

        reformat(file)

        TestCase.assertEquals(unformatted.replace("{-caret-}", "").trimIndent(), document.text.trimIndent())
        TestCase.assertEquals(org.elm.ide.actions.ElmExternalFormatAction.FileHasErrorsNotificationMsg, notificationRef.get().content)
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

    fun `test elm-format action should add to the undo stack`() {
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
        reformat(file)

        val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)

        val undoManager = UndoManager.getInstance(project)
        TestCase.assertTrue(undoManager.isUndoAvailable(fileEditor))
    }

    private fun reformat(file: VirtualFile) {
        val (action, event) = makeTestAction(file)
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

    private fun connectToBusAndGetNotificationRef(): Ref<Notification> {
        val notificationRef = Ref<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC,
                object : NotificationsAdapter() {
                    override fun notify(notification: Notification) =
                            notificationRef.set(notification)
                })
        return notificationRef
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