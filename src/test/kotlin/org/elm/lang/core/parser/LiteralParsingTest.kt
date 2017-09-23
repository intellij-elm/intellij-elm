package org.elm.lang.core.parser

import com.intellij.testFramework.ParsingTestCase


class LiteralParsingTest : ParsingTestCase("", "elm", ElmParserDefinition()) {

    fun testEmptyString() {
        doTest(true)
    }

    fun testFloat() {
        doTest(true)
    }

    fun testInt() {
        doTest(true)
    }

    fun testNegativeInt() {
        doTest(true)
    }

    fun testLongInt() {
        doTest(true)
    }

    fun testMultiLineString() {
        doTest(true)
    }

    fun testQuotedString() {
        doTest(true)
    }

    fun testStrangeFloat() {
        doTest(true)
    }

    fun testString() {
        doTest(true)
    }

    fun testChar() {
        doTest(true)
    }

    fun testEscapedChar() {
        doTest(true)
    }

    override fun getTestDataPath(): String {
        return "src/test/resources/org/elm/lang/core/parsing/fixtures/literals"
    }

    override fun skipSpaces(): Boolean {
        return false
    }

    override fun includeRanges(): Boolean {
        return true
    }
}
