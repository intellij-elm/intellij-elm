package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer

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

    fun testBasics() = doTest()
    fun testHeader() = doTest()
    fun testComments() = doTest()
    fun testEmptyModule() = doTest()
    fun testLetIn() = doTest()
    fun testLetInPartial() = doTest()
    fun testLetInSingleLineBug() = doTest()
    fun testLetInSingleLineBug2() = doTest()
    fun testCaseOf() = doTest()
    fun testCaseFollowedByTopLevelDecl() = doTest()
    fun testCaseOfPartial() = doTest()
    fun testTypes() = doTest()
    fun testTypeAnnotations() = doTest()
    fun testWhitespaceHandling() = doTest()
}
