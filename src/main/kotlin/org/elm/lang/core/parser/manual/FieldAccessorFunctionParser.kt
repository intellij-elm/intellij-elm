package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import org.elm.lang.core.psi.ElmTypes.DOT
import org.elm.lang.core.psi.ElmTypes.FIELD_ACCESSOR_FUNCTION
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER

/**
 * Parses a special function, synthesized by the Elm compiler, that reads
 * the value of a record field.
 *
 * Parses the following grammar rule, with NO whitespace between the tokens:
 *
 * `field_accessor_function ::= DOT LOWER_CASE_IDENTIFIER`
 */
class FieldAccessorFunctionParser : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "field_accessor_function"))
            return false

        if (builder.rawLookup(0) !== DOT
                || builder.rawLookup(1) !== LOWER_CASE_IDENTIFIER
                || builder.rawLookup(2) === DOT)
            return false

        val marker = enter_section_(builder)
        val result = consumeTokens(builder, 0, DOT, LOWER_CASE_IDENTIFIER)
        exit_section_(builder, marker, FIELD_ACCESSOR_FUNCTION, result)
        return result
    }
}
