package org.elm.lang.core.parser

import com.intellij.testFramework.ParsingTestCase

class CommentParsingTest : ParsingTestCase("", "elm", ElmParserDefinition()) {

    fun testLine() {
        doTest(true)
    }

    fun testBlock() {
        doTest(true)
    }

    fun testDoc() {
        doTest(true)
    }

    override fun getTestDataPath(): String {
        return "src/test/resources/org/elm/lang/core/parsing/fixtures/comments"
    }

    override fun skipSpaces(): Boolean {
        return false
    }

    override fun includeRanges(): Boolean {
        return true
    }
}