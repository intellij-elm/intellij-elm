package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmConstantTag
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * The double-quote character used to delimit regular (non-triple-quoted) strings.
 */
const val REGULAR_STRING_DELIMITER = "\""

/**
 * The three double-quote characters used to delimit triple-quoted strings. See [ElmStringConstantExpr.isTripleQuoted]
 * for more info on this.
 */
const val TRIPLE_QUOTE_STRING_DELIMITER = "\"\"\""

/** A literal string. e.g. `""` or `"""a"b"""` */
class ElmStringConstantExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmConstantTag {

    /**
     * Indicates whether this is a "triple-quoted" string, i.e. a string with `"""` as its delimiters. Typically this is
     * used for multi-line strings, but it doesn't have to be: it can be used for single line strings as the text within
     * it can contain double-quotes without needing to escape them.
     */
    val isTripleQuoted: Boolean
        get() = node.text.startsWith(TRIPLE_QUOTE_STRING_DELIMITER) &&
                node.text.endsWith(TRIPLE_QUOTE_STRING_DELIMITER) &&
                node.text.length >= 6

    /**
     * The content of this string constant, i.e. the text inside its delimiters/quotes.
     */
    val textContent: String
        get() {
            val delimiter = if (isTripleQuoted) TRIPLE_QUOTE_STRING_DELIMITER else REGULAR_STRING_DELIMITER
            val delimiterLength = delimiter.length
            return node.text.substring(delimiterLength, node.text.length - delimiterLength)
        }
}
