package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.*
import com.intellij.psi.TokenType
import org.elm.lang.core.psi.ElmTypes.*

/**
 * Parses a `.` token iif it does not have whitespace on either side of it.
 *
 * Does not emit any markers
 */
object DotWithoutWhitespaceParser : GeneratedParserUtilBase.Parser {
    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "dot_without_ws")
                || builder.rawLookup(-1) === TokenType.WHITE_SPACE
                || builder.rawLookup(0) !== DOT
                || builder.rawLookup(1) === TokenType.WHITE_SPACE) {
            return false
        }

        return parseTokens(builder, 0, DOT)
    }
}
