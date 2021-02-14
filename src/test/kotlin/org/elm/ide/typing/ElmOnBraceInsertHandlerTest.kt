package org.elm.ide.typing

import org.intellij.lang.annotations.Language


class ElmOnBraceInsertHandlerTest : ElmTypingTestBase() {

    fun `test add parentheses around selection`() = doTest("""
f = <selection>4 + 3</selection>
""", """
f = (4 + 3)
""")

    fun `test add braces around selection`() = doTest("""
f = <selection>name</selection>
""", """
f = {name}
""", '{')

    fun `test does not interfere with default brace behaviour`() = doTest("""
f = {-caret-}
""", """
f = ()
""")

    fun doTest(@Language("Elm") before: String, @Language("Elm") after: String, c: Char = '(') {
        checkByText(before, after) {
            myFixture.type(c)
        }
    }
}
