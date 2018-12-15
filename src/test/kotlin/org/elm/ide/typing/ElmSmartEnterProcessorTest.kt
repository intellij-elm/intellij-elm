package org.elm.ide.typing

import com.intellij.openapi.actionSystem.IdeActions
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

class ElmSmartEnterProcessorTest : ElmTestBase() {
    fun `test fix missing case branches`() = doTest("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of{-caret-}
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar ->{-caret-}

        Baz ->

        Qux ->
""")


    private fun doTest(@Language("Elm") before: String, @Language("Elm") after: String) =
            checkByText(before, after) {
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
            }
}
