package org.elm.ide.spelling

import com.intellij.spellchecker.inspections.SpellCheckingInspection
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test

class ElmSpellCheckerTest : ElmTestBase() {


    @Test
    fun testNamedElement() = doTest(
            """<TYPO descr="Typo: In word 'wodlr'">wodlr</TYPO> = 42""")


    @Test
    fun testComments() = doTest(
            """-- Hello, <TYPO descr="Typo: In word 'Wodrl'">Wodrl</TYPO>!""")


    @Test
    fun testStringLiterals() = doTest(
            """x = "<TYPO descr="Typo: In word 'Wodlr'">Wodlr</TYPO>" """)


    @Test
    fun testCommentsSuppressed() = doTest(
            "-- Hello, Wodrl!",
            processComments = false)


    @Test
    fun testStringLiteralsSuppressed() = doTest(
            """x = "Hello, Wodlr!" """,
            processLiterals = false)


    private fun doTest(@Language("Elm") text: String, processComments: Boolean = true, processLiterals: Boolean = true) {
        val inspection = SpellCheckingInspection()
        inspection.processLiterals = processLiterals
        inspection.processComments = processComments

        myFixture.configureByText("main.elm", text)
        myFixture.enableInspections(inspection)
        myFixture.testHighlighting(false, false, true, "main.elm")
    }
}
