package org.elm.lang.core.psi

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import org.elm.fileTreeFromText
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmGlobalModificationTrackerTest.TestAction.INC
import org.intellij.lang.annotations.Language

internal class ElmGlobalModificationTrackerTest : ElmTestBase() {
    fun `test comment`() = doTest(TestAction.NOT_INC, """
-- {-caret-}
""")

    fun `test function`() = doTest(INC, """
{-caret-}
""", "main = ()")

    fun `test unannotated function name`() = doTest(INC, """
main{-caret-} = ()
""")

    fun `test unannotated function params`() = doTest(INC, """
main a {-caret-} = ()
""")

    fun `test unannotated function body`() = doTest(INC, """
main = {-caret-}
""", "()")

    fun `test annotation`() = doTest(INC, """
main : () -> {-caret-}
main a = a
""", "()")

    fun `test annotated function name`() = doTest(INC, """
main : ()
main{-caret-} = ()
""")

    fun `test annotated function params`() = doTest(TestAction.NOT_INC, """
main : () -> ()
main a {-caret-} = ()
""")

    fun `test annotated function body`() = doTest(TestAction.NOT_INC, """
main : ()
main = {-caret-}
""", "()")

    fun `test nested function name in annotated parent`() = doTest(TestAction.NOT_INC, """
main : ()
main =
    let
        foo{-caret-} = ()
    in
    foo
""", "()")

    fun `test nested function body in annotated parent`() = doTest(TestAction.NOT_INC, """
main : ()
main =
    let
        foo = {-caret-}
    in
    foo
""", "()")

    fun `test type`() = doTest(INC, """
type T = T {-caret-}
""")

    fun `test type alias`() = doTest(INC, """
type alias R = { {-caret-} }
""")

    fun `test replace function with comment`() = doTest(INC, """
main : ()
{-caret-}main = ()
""", "-- ")


    fun `test vfs file change`() = doVfsTest(INC, """
--@ Main.elm
import Foo exposing (..)
--^
--@ Foo.elm
module Foo exposing (..)
-- foo = ()
""") { file ->
        VfsUtil.saveText(file, VfsUtil.loadText(file).replace("--", ""))
    }

    fun `test vfs file removal`() = doVfsTest(INC, """
--@ Main.elm
import Foo exposing (..)
--^
--@ Foo.elm
module Foo exposing (..)
foo = ()
""") { file ->
        file.delete(null)
    }

    fun `test vfs file rename`() = doVfsTest(INC, """
--@ Main.elm
import Foo exposing (..)
--^
--@ Foo.elm
module Foo exposing (..)
foo = ()
""") { file ->
        file.rename(null, "Bar.elm")
    }

    fun `test vfs directory removal`() = doVfsTest(INC, """
--@ Main.elm
import Foo.Bar exposing (..)
--^
--@ Foo/Bar.elm
module Bar exposing (..)
foo = ()
""", "Foo") { file ->
        file.delete(null)
    }

    private enum class TestAction(val function: (Long, Long) -> Boolean, val comment: String) {
        INC({ a, b -> a > b }, "Modification counter expected to be incremented, but it remained the same"),
        NOT_INC({ a, b -> a == b }, "Modification counter expected to remain the same, but it was incremented")
    }

    private fun checkModCount(op: TestAction, action: () -> Unit) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val modTracker = project.modificationTracker
        val oldCount = modTracker.modificationCount
        action()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        check(op.function(modTracker.modificationCount, oldCount)) { op.comment }
    }

    private fun checkModCount(op: TestAction, @Language("Elm") code: String, text: String) {
        InlineFile(code).withCaret()
        checkModCount(op) { myFixture.type(text) }
    }

    private fun doTest(op: TestAction, @Language("Elm") code: String, text: String = "a") {
        checkModCount(op, code, text)
    }

    private fun doVfsTest(op: TestAction, @Language("Elm") code: String, filename: String = "Foo.elm", action: (VirtualFile) -> Unit) {
        val p = fileTreeFromText(code).createAndOpenFileWithCaretMarker()
        val file = p.psiFile(filename).virtualFile!!
        checkModCount(op) {
            runWriteAction {
                action(file)
            }
        }
    }
}

