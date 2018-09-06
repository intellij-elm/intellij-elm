package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.*
import com.intellij.psi.TokenType
import org.elm.lang.core.psi.ElmTypes.*

/**
 * Parses the following grammar rule:
 *
 * ```
 * expression_accessor ::= DOT [LOWER_CASE_IDENTIFIER DOT]* LOWER_CASE_IDENTIFIER
 * ```
 * This parser will fail if preceded by whitespace. Used for parsing the accessor part of expressions like:
 *
 * ```
 * (record).field1.field2
 * {x=1}.x
 * ```
 */
class ExpressionAccessorParser : GeneratedParserUtilBase.Parser {
    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "value_qid"))
            return false

        if (builder.rawLookup(-1) === TokenType.WHITE_SPACE
                || builder.rawLookup(0) !== DOT
                || builder.rawLookup(1) !== LOWER_CASE_IDENTIFIER) {
            return false
        }

        val marker = enter_section_(builder)

        val result = parseAccessorParts(builder, level + 1)

        exit_section_(builder, marker, EXPRESSION_ACCESSOR, result)
        return result
    }

    private fun parseAccessorParts(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "value_qid"))
            return false

        var c = current_position_(builder)
        var result = true
        while (result) {
            if (builder.rawLookup(0) !== DOT || builder.rawLookup(1) !== LOWER_CASE_IDENTIFIER)
                break

            result = parseTokens(builder, 0, DOT, LOWER_CASE_IDENTIFIER)
            if (!empty_element_parsed_guard_(builder, "value_qid", c))
                break
            c = current_position_(builder)
        }
        return result
    }
}
