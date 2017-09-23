package org.elm.lang.core.parser

import com.intellij.testFramework.ParsingTestCase


class ImportParsingTest : ParsingTestCase("", "elm", ElmParserDefinition()) {

    fun testSimplest() {
        doTest(true)
    }

    fun testAlias() {
        doTest(true)
    }

    fun testExposingAll() {
        doTest(true)
    }

    fun testExposingSomeMembers() {
        doTest(true)
    }

    fun testPathName() {
        doTest(true)
    }

    fun testBrokenLines() {
        doTest(true)
    }

    override fun getTestDataPath(): String {
        return "src/test/resources/org/elm/lang/core/parsing/fixtures/import"
    }

    override fun skipSpaces(): Boolean {
        return false
    }

    override fun includeRanges(): Boolean {
        return true
    }
}