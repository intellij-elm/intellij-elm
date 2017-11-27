package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.DOT
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.FIELD_ACCESS

/**
 * Parses a chain of one or more record fields being de-referenced from a base record value.
 *
 * Implements a parser for the following grammar rule, with NO whitespace between the tokens:
 *
 * `field_access ::= LOWER_CASE_IDENTIFIER DOT LOWER_CASE_IDENTIFIER [DOT LOWER_CASE_IDENTIFIER]*`
 */
class FieldAccessParser : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "field_access"))
            return false

        if (builder.rawLookup(0) !== LOWER_CASE_IDENTIFIER
            || builder.rawLookup(1) !== DOT
            || builder.rawLookup(2) !== LOWER_CASE_IDENTIFIER)
            return false

        val marker = enter_section_(builder)
        var result: Boolean

        result = consumeToken(builder, LOWER_CASE_IDENTIFIER)
        result = result && consumeToken(builder, DOT)
        result = result && consumeToken(builder, LOWER_CASE_IDENTIFIER)

        val isContinued = builder.rawLookup(0) === DOT // TODO [kl] I think I can get rid of this line
        result = result && (!isContinued || listOfMembers(builder, level + 1))

        exit_section_(builder, marker, FIELD_ACCESS, result)
        return result
    }

    private fun listOfMembers(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "field_access"))
            return false

        var c = current_position_(builder)
        var result = true
        while (true) {
            if (builder.rawLookup(0) !== DOT || builder.rawLookup(1) !== LOWER_CASE_IDENTIFIER)
                break

            result = parseTokens(builder, 0, DOT, LOWER_CASE_IDENTIFIER)
            if (!empty_element_parsed_guard_(builder, "field_access", c))
                break
            c = current_position_(builder)
            if (!result)
                break
        }
        return result
    }
}