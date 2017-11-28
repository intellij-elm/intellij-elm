package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer

/**
 * Test the lexer's ability to synthesize tokens related to offside rule.
 */
class ElmLayoutLexerTest: ElmLexerTestCaseBase() {

    override fun getTestDataPath() =
            "org/elm/lang/core/lexer/fixtures"

    override fun createLexer(): Lexer {
        return ElmLexer(ElmIncrementalLexer())
    }

    fun testBasics() = doTest()
    fun testLineComments() = doTest()
    fun testBlockComments() = doTest()
    fun testDocComments() = doTest()
    fun testLetIn() = doTest()
    fun testLetInSingleLineBug() = doTest()
    fun testCaseOf() = doTest()
}
