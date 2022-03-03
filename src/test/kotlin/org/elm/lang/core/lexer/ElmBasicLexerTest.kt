package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer

/**
 * Test the basic/incremental JFlex lexer
 *
 * @see ElmIncrementalLexer
 */
class ElmBasicLexerTest: ElmLexerTestCaseBase() {

    override fun getTestDataPath() =
            "org/elm/lang/core/lexer/basic/fixtures"

    override fun createLexer(): Lexer {
        return ElmIncrementalLexer()
    }

    // TODO [kl] add more tests
    fun testChars() = doTest()
    fun testLineComments() = doTest()
    fun testBlockComments() = doTest()
    fun testDocComments() = doTest()
    fun testStrings() = doTest()
    fun testNumbers() = doTest()
    fun testShaders() = doTest()
    fun testPartialSingleQuoteStrings() = doTest()
    fun testPartialTripleQuoteString() = doTest()
    fun testTypes() = doTest()
}
