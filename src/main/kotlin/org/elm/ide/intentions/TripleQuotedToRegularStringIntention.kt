package org.elm.ide.intentions

import org.elm.lang.core.psi.elements.ElmStringConstantExpr
import org.elm.lang.core.psi.elements.REGULAR_STRING_DELIMITER

/**
 * An intention which converts triple-quoted string to regular strings, e.g. replaces this:
 * ```
 *     example =
 *         """foo
 *     bar"""
 * ```
 * with this:
 * ```
 *     example =
 *         "foo\nbar"
 * ```
 */
class TripleQuotedToRegularStringIntention : StringDelimiterIntention(REGULAR_STRING_DELIMITER) {

    override fun getText() = "Convert to regular string"

    override val ElmStringConstantExpr.isValidForReplacement: Boolean
        get() = isTripleQuoted

    override fun getReplacement(source: String): String {
        // In triple-quoted strings, double-quotes don't need to be escaped, unless this is the last character.
        // So if this ends with an escaped double-quote, replace it with a unescaped double-quote so it's the same
        // as the rest of the double quotes which we'll escape soon.
        return source
            .unescapeLastDoubleQuote()
            // Replace line separators with "\n" as they need to be escaped in regular strings
            .replace(System.lineSeparator(), SLASH_N)
            // Escape double-quotes.
            .replace(DOUBLE_QUOTE, ESCAPED_DOUBLE_QUOTE)
    }

    private fun String.unescapeLastDoubleQuote() =
        if (endsWith(ESCAPED_DOUBLE_QUOTE)) removeSuffix(ESCAPED_DOUBLE_QUOTE) + DOUBLE_QUOTE
        else this
}
