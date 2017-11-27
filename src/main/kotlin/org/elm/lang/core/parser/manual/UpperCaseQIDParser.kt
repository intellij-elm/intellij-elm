package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.DOT
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_QID

/**
 * Parses the following grammar rule, with NO whitespace in between:
 *
 * upper_case_qid ::= UPPER_CASE_IDENTIFIER [DOT UPPER_CASE_IDENTIFIER]*
 */
class UpperCaseQIDParser : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "upper_case_qid"))
            return false

        if (!nextTokenIs(builder, UPPER_CASE_IDENTIFIER))
            return false

        var result: Boolean
        val isContinued = builder.rawLookup(1) === DOT
        val marker = enter_section_(builder)

        result = consumeToken(builder, UPPER_CASE_IDENTIFIER)
        result = result && (!isContinued || listOfMembers(builder, level + 1))

        exit_section_(builder, marker, UPPER_CASE_QID, result)
        return result
    }

    private fun listOfMembers(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "upper_case_qid"))
            return false

        var c = current_position_(builder)
        var result = true
        while (true) {
            if (builder.rawLookup(0) !== DOT || builder.rawLookup(1) !== UPPER_CASE_IDENTIFIER)
                break

            result = parseTokens(builder, 0, DOT, UPPER_CASE_IDENTIFIER)
            if (!empty_element_parsed_guard_(builder, "upper_case_qid", c))
                break
            c = current_position_(builder)
            if (!result)
                break
        }
        return result
    }
}

