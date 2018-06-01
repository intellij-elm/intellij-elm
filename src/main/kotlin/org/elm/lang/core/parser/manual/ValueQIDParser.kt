package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.*

/**
 * Parses the following grammar rule:
 *
 * value_qid ::= [UPPER_CASE_IDENTIFIER DOT]* LOWER_CASE_IDENTIFIER
 */
class ValueQIDParser : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "value_qid"))
            return false

        val startsWithQualifier = nextTokenIs(builder, UPPER_CASE_IDENTIFIER)
                && builder.rawLookup(1) === DOT

        if (!startsWithQualifier && !nextTokenIs(builder, LOWER_CASE_IDENTIFIER))
            return false

        val marker = enter_section_(builder)
        var result = true

        if (startsWithQualifier) {
            result = listOfMembers(builder, level + 1)
        }

        result = result && consumeToken(builder, LOWER_CASE_IDENTIFIER)

        exit_section_(builder, marker, VALUE_QID, result)
        return result
    }

    private fun listOfMembers(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "value_qid"))
            return false

        var c = current_position_(builder)
        var result = true
        while (true) {
            if (builder.rawLookup(0) !== UPPER_CASE_IDENTIFIER || builder.rawLookup(1) !== DOT)
                break

            result = parseTokens(builder, 0, UPPER_CASE_IDENTIFIER, DOT)
            if (!empty_element_parsed_guard_(builder, "value_qid", c))
                break
            c = current_position_(builder)
            if (!result || builder.rawLookup(0) !== UPPER_CASE_IDENTIFIER)
                break
        }
        return result
    }
}