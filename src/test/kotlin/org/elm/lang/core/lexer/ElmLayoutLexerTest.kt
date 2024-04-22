package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer
import org.junit.Test

/**
 * Test the lexer's ability to synthesize tokens related to offside rule.
 *
 * @see ElmLayoutLexer
 */
class ElmLayoutLexerTest: ElmLexerTestCaseBase() {

    override fun getTestDataPath() =
            "org/elm/lang/core/lexer/layout/fixtures"

    override fun createLexer(): Lexer {
        return ElmLayoutLexer(ElmIncrementalLexer())
    }

    override fun shouldTrim(): Boolean {
        // do not trim newlines at the end because it can hide EOF bugs
        return false
    }

    @Test
    fun testBasics() = doTest()
    @Test
    fun testHeader() = doTest()
    @Test
    fun testComments() = doTest()
    @Test
    fun testEmptyModule() = doTest()
    @Test
    fun testLetIn() = doTest()
    @Test
    fun testLetInPartial() = doTest()
    @Test
    fun testLetInSingleLineBug() = doTest()
    @Test
    fun testLetInSingleLineBug2() = doTest()
    @Test
    fun testCaseOf() = doTest()
    @Test
    fun testCaseFollowedByTopLevelDecl() = doTest()
    @Test
    fun testCaseOfPartial() = doTest()
    @Test
    fun testTypes() = doTest()
    @Test
    fun testTypeAnnotations() = doTest()
    @Test
    fun testWhitespaceHandling() = doTest()
}
