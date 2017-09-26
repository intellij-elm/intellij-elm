package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.*

class EffectParser : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "EffectParser"))
            return false

        if (builder.rawLookup(0) !== LOWER_CASE_IDENTIFIER || "effect" != builder.tokenText)
            return false

        val result: Boolean
        val marker = enter_section_(builder)
        result = consumeTokens(builder, 0, LOWER_CASE_IDENTIFIER)
        exit_section_(builder, marker, EFFECT, result)
        return result
    }
}