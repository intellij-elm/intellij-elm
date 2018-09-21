package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.parseTokens
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.psi.TokenType
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER

/**
 * Parses a `-` token iif it is not followed by whitespace.
 *
 * Does not emit any markers
 */
object MinusWithoutTrailingWhitespaceParser : GeneratedParserUtilBase.Parser {
    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "minus_without_ws")
                || builder.tokenText != "-"
                || builder.rawLookup(1) === TokenType.WHITE_SPACE) {
            return false
        }

        return parseTokens(builder, 0, OPERATOR_IDENTIFIER)
    }
}
