package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import org.elm.lang.core.psi.ElmTypes.IN
import org.elm.lang.core.psi.ElmTypes.LET
import org.elm.lang.core.psi.ElmTypes.LET_IN

class LetInParser(val innerValueDeclaration: Parser,
                  val otherValueDeclarations: Parser,
                  val expression: Parser
) : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "let_in"))
            return false

        if (!nextTokenIs(builder, LET))
            return false

        var result: Boolean
        val marker = enter_section_(builder)
        result = consumeToken(builder, LET)

        result = result && IndentationTokenTypeRemapper.use({reMapper, resultInner ->
            builder.setTokenTypeRemapper(reMapper)
            if (resultInner) {
                val indentationValue = IndentationHelper.getIndentationOfPreviousToken(builder)
                reMapper.pushIndentation(indentationValue)
            }
            return@use (resultInner
                    && this@LetInParser.innerValueDeclaration.parse(builder, level + 1)
                    && this@LetInParser.otherValueDeclarations.parse(builder, level + 1)
                    && consumeToken(builder, IN))
        }, result)

        result = result && this.expression.parse(builder, level + 1)
        exit_section_(builder, marker, LET_IN, result)
        return result
    }
}
