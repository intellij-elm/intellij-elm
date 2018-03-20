package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer

/**
 * Test the lexer's ability to synthesize tokens related to offside rule.
 */
class ElmLayoutLexerTest: ElmLayoutLexerTestCaseBase() {

    override fun getTestDataPath() =
            "org/elm/lang/core/lexer/fixtures"

    override fun createLexer(): Lexer {
        return ElmLayoutLexer(ElmIncrementalLexer())
    }

    fun testBasics() = doTest()
    fun testHeader() = doTest()
    fun testPortModule() = doTest()
    fun testEffectModule() = doTest()
    fun testLineComments() = doTest()
    fun testBlockComments() = doTest()
    fun testDocComments() = doTest()
    fun testLetIn() = doTest()
    fun testLetInSingleLineBug() = doTest()
    fun testCaseOf() = doTest()
    fun testTypes() = doTest()
    fun testTypeAnnotations() = doTest()
}
