package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.parseTokens
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.psi.TokenType
import org.elm.lang.core.psi.ElmTypes.DOT

/**
 * Parses a `.` token, optionally forbidding whitespace on one or both sides.
 *
 * Does not emit any markers
 */
class DotWithoutWhitespaceParser(private val allowLeadingWs:Boolean,
                                 private val allowTrailingWs:Boolean) : GeneratedParserUtilBase.Parser {
    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "dot_without_ws")
                || (!allowLeadingWs && builder.rawLookup(-1) === TokenType.WHITE_SPACE)
                || builder.rawLookup(0) !== DOT
                || (!allowTrailingWs && builder.rawLookup(1) === TokenType.WHITE_SPACE)) {
            return false
        }

        return parseTokens(builder, 0, DOT)
    }
}
