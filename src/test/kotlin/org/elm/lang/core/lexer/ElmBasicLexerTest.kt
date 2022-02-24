package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer
import org.junit.Test

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
    @Test
    fun testChars() = doTest()
    @Test
    fun testLineComments() = doTest()
    @Test
    fun testBlockComments() = doTest()
    @Test
    fun testDocComments() = doTest()
    @Test
    fun testStrings() = doTest()
    @Test
    fun testNumbers() = doTest()
    @Test
    fun testShaders() = doTest()
    @Test
    fun testPartialSingleQuoteStrings() = doTest()
    @Test
    fun testPartialTripleQuoteString() = doTest()
    @Test
    fun testTypes() = doTest()
}
