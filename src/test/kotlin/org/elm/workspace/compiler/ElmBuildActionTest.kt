package org.elm.workspace.compiler

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import junit.framework.TestCase
import org.elm.workspace.ElmWorkspaceTestBase
import org.intellij.lang.annotations.Language
import java.nio.file.Path

class ElmBuildActionTest : ElmWorkspaceTestBase() {

    fun `test build Elm application project`() {
        val source = """
                    module Main exposing (..)
                    import Html
                    main = Html.text "hi"
                """.trimIndent()

        buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", source)
            }
        }
        val file = myFixture.configureFromTempProjectFile("src/Main.elm").virtualFile
        doTest(file, expectedNumErrors = 0, expectedOffset = source.indexOf("main"))
    }


    fun `test build Elm application project with an error`() {
        val source = """
                    module Main exposing (..)
                    import Html
                    foo = bogus
                    main = Html.text "hi"
                """.trimIndent()

        buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", source)
            }
        }
        val file = myFixture.configureFromTempProjectFile("src/Main.elm").virtualFile
        doTest(file, expectedNumErrors = 1, expectedOffset = source.indexOf("main"))
    }

    fun `test build Elm project ignores nested function named 'main'`() {
        val source = """
                    module Main exposing (..)
                    import Html
                    foo =
                        let
                            main = Html.text "hi"
                        in
                        main
                        
                """.trimIndent()

        buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Foo.elm", source)
            }
        }
        val file = myFixture.configureFromTempProjectFile("src/Foo.elm").virtualFile
        doTestShowsErrorBalloon(file, "Cannot find your Elm app's main entry point")
    }


    private fun doTest(file: VirtualFile, expectedNumErrors: Int, expectedOffset: Int) {
        var succeeded = false
        with(project.messageBus.connect(testRootDisposable)) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String, offset: Int) {
                    TestCase.assertEquals(expectedNumErrors, messages.size)
                    TestCase.assertEquals("src/Main.elm", targetPath)
                    TestCase.assertEquals(expectedOffset, offset)
                    succeeded = true
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

    private fun doTestShowsErrorBalloon(file: VirtualFile, errorFragment: String) {
        val (action, event) = makeTestAction(file)
        check(event.presentation.isEnabledAndVisible) {
            "The build action should be enabled in this context"
        }
        val ref = connectToBusAndGetNotificationRef()
        action.actionPerformed(event)
        TestCase.assertTrue(ref.get().content.contains(errorFragment))
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
            "elm-version": "0.19.1",
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
