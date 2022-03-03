package org.elm.ide.intentions

import org.elm.lang.core.psi.elements.TRIPLE_QUOTE_STRING_DELIMITER

class TripleQuotedToRegularStringIntentionTest : ElmIntentionTestBase(TripleQuotedToRegularStringIntention()) {

    // Note that because Kotlin and Elm both use """, we can't put """ in the embedded Elm code snippets below, as the
    // Kotlin compiler assumes these terminate the Kotlin string literal. And raw Kotlin string literals can't have
    // escape characters in them. So instead use the TRIPLE_QUOTE_STRING_DELIMITER constant.

    fun `test converts non-empty triple-quoted string to regular string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}ab{-caret-}cd$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                "abcd"
        """.trimIndent()
    )

    fun `test converts empty triple-quoted string to regular string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                $TRIPLE_QUOTE_STRING_DELIMITER{-caret-}$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                ""
        """.trimIndent()
    )

    fun `test converts white-space only triple-quoted string to regular string`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                $TRIPLE_QUOTE_STRING_DELIMITER  {-caret-}   $TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                "     "
        """.trimIndent()
    )

    fun `test converts carriage returns and quotes`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                ${TRIPLE_QUOTE_STRING_DELIMITER}foo
            "bar"{-caret-} baz$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                "foo\n\"bar\" baz"
        """.trimIndent()
    )

    // Last quote in a triple-quoted string is escaped, the rest aren't.
    fun `test quotes in text`() = doAvailableTest(
        """
            module Foo exposing (s0)
            s0 =
                $TRIPLE_QUOTE_STRING_DELIMITER"fo{-caret-}"o\"$TRIPLE_QUOTE_STRING_DELIMITER
        """.trimIndent(),

        """
            module Foo exposing (s0)
            s0 =
                "\"fo\"o\""
        """.trimIndent()
    )

    fun `test unavailable for regular-quoted string`() = doUnavailableTest(
        """
            module Foo exposing (s0)
            s0 =
                "ab{-caret-}cd"
        """.trimIndent()
    )
}
