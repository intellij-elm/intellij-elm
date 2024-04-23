package org.elm.ide.typing

import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test


class ElmOnBraceInsertHandlerTest : ElmTestBase() {

    @Test
    fun `test add parentheses around selection`() = doTest("""
f = <selection>4 + 3</selection>
""", """
f = (4 + 3)
""")

    @Test
    fun `test add braces around selection`() = doTest("""
f = <selection>name</selection>
""", """
f = {name}
""", '{')

    @Test
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
