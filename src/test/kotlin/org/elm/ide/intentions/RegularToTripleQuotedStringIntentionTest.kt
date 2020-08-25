package org.elm.ide.intentions

import org.elm.lang.core.psi.elements.TRIPLE_QUOTE_STRING_DELIMITER

class RegularToTripleQuotedStringIntentionTest : ElmIntentionTestBase(RegularToTripleQuotedStringIntention()) {

    // Note that because Kotlin and Elm both use """, we can't put """ in the embedded Elm code snippets below, as the
    // Kotlin compiler assumes these terminate the Kotlin string literal. And raw Kotlin string literals can't have
    // escape characters in them. So instead use the TRIPLE_QUOTE_STRING_DELIMITER constant.

    fun `test converts non-empty regular string to triple-quoted string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "ab{-caret-}cd"
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}abcd$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )

    fun `test converts empty regular string to triple-quoted string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "{-caret-}"
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )

    fun `test converts white-space only regular string to triple-quoted string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "  {-caret-}   "
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                $TRIPLE_QUOTE_STRING_DELIMITER     $TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )

    fun `test converts carriage returns and quotes`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "foo\n\"bar\"{-caret-} baz"
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}foo
            "bar" baz$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )

    // If string ends in quotes then when converted to triple-quoted string, the last quote should be escaped, but any
    // other quotes shouldn't.
    fun `test quotes in text`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "\"fo{-caret-}\"o\""
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                $TRIPLE_QUOTE_STRING_DELIMITER"fo"o\"$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )

    fun `test unavailable for triple-quoted string`() = doUnavailableTest(
        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}ab{-caret-}cd$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent()
    )
}
