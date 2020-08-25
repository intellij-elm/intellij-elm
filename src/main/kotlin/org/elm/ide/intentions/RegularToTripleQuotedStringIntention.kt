package org.elm.ide.intentions

import org.elm.lang.core.psi.elements.ElmStringConstantExpr
import org.elm.lang.core.psi.elements.TRIPLE_QUOTE_STRING_DELIMITER

/**
 * An intention which converts regular strings to triple-quoted strings, e.g. replaces this:
 * ```
 *     example =
 *         "foo\nbar"
 * ```
 * with this:
 * ```
 *     example =
 *         """foo
 *     bar"""
 * ```
 */
class RegularToTripleQuotedStringIntention : StringDelimiterIntention(TRIPLE_QUOTE_STRING_DELIMITER) {

    override fun getText() = "Convert to triple-quoted string"

    override val ElmStringConstantExpr.isValidForReplacement: Boolean
        get() = !isTripleQuoted

    override fun getReplacement(source: String): String {
        val replacement = source
            // Replace \n with an actual line separator as they don't need to be escaped in triple-quoted strings.
            .replace(SLASH_N, System.lineSeparator())
            // Replace \" with double-quotes without the backslash as they don't need to be escaped either.
            .replace(ESCAPED_DOUBLE_QUOTE, DOUBLE_QUOTE)

        // There is one exception to the comment above: if a string ends in a double-quote, the last double-quotes need
        // to be escaped.
        return if (replacement.endsWith(DOUBLE_QUOTE)) replacement.removeSuffix(DOUBLE_QUOTE) + ESCAPED_DOUBLE_QUOTE
        else replacement
    }
}
