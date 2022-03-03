package org.elm.ide.actions

import org.elm.TestProject
import org.elm.openapiext.runWriteCommandAction
import org.elm.workspace.ElmWorkspaceTestBase


class ElmCreateFileActionTest : ElmWorkspaceTestBase() {

    fun `test file creation in a root src-dir`() =
            doTest("src", "Quux", "module Quux exposing (..)")

    fun `test file creation within a sub-dir`() =
            doTest("src/Foo", "Quux", "module Foo.Quux exposing (..)")

    fun `test file creation within a deeper source root`() =
            doTest("vendor/elm-foo", "Bar", "module Bar exposing (..)")

    fun `test file creation within a deeper source root and within a sub-dir`() =
            doTest("vendor/elm-foo/Internals", "Baz", "module Internals.Baz exposing (..)")

    fun `test file creation in root of 'tests' directory`() =
            doTest("tests", "Quux", "module Quux exposing (..)")

    fun `test file creation in sub-dir of 'tests' directory`() =
            doTest("tests/Legacy", "Quux", "module Legacy.Quux exposing (..)")

    fun `test file creation outside of a source root uses an empty module qualifier`() =
            doTest("outside", "Quux", "module Quux exposing (..)")

    // https://github.com/klazuka/intellij-elm/issues/231
    fun `test file creation including file extension`() =
            doTest("src", "Quux.elm", "module Quux exposing (..)")

    // https://github.com/klazuka/intellij-elm/issues/202
    fun `test normalization of leading dot-slash in source-directory`() =
            doTest("foo1/Foo1", "Quux", "module Foo1.Quux exposing (..)")

    private fun doTest(dirPath: String, name: String, expectedContents: String) {
        val testProject = makeTestProjectFixture()
        val action = ElmCreateFileAction()
        val dirVirtualFile = testProject.root.findFileByRelativePath(dirPath)!!
        myFixture.project.runWriteCommandAction {
            action.testHelperCreateFile(name, myFixture.psiManager.findDirectory(dirVirtualFile)!!)
        }
        val filename = name.removeSuffix(".elm")
        myFixture.checkResult("$dirPath/$filename.elm", expectedContents, true)
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
                    dir("Foo") {}
                    dir("Bar") {}
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
