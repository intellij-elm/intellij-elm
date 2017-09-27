package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.CASE
import org.elm.lang.core.psi.ElmTypes.CASE_OF

class CaseOfParser(private val header: Parser,
                   private val branch: Parser,
                   private val oneOrMoreSeparations: Parser
) : GeneratedParserUtilBase.Parser {

    private var indentation: Int = 0

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "CaseOfParser")) {
            return false
        }
        if (!nextTokenIs(builder, CASE)) {
            return false
        }
        var result: Boolean
        val marker = enter_section_(builder)
        result = this.header.parse(builder, level + 1)

        result = result && IndentationTokenTypeRemapper.use({ reMapper, resultInner ->
            indentation = IndentationHelper.getIndentationOfPreviousToken(builder)
            reMapper.pushIndentation(indentation)
            builder.setTokenTypeRemapper(reMapper)
            return@use (resultInner
                    && branch.parse(builder, level + 1)
                    && separatedBranches(builder, level + 1))
        }, result)

        exit_section_(builder, marker, CASE_OF, result)
        return result
    }

    // (case_of_one_or_more_separation case_of_branch)*
    private fun separatedBranches(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "separatedBranches")) {
            return false
        }
        var currentPosition = current_position_(builder)
        while (true) {
            if (!this.separatedBranch(builder, level + 1)) break
            if (!empty_element_parsed_guard_(builder, "separatedBranches", currentPosition)) break
            currentPosition = current_position_(builder)
        }
        return true
    }

    // case_of_one_or_more_separation case_of_branch
    private fun separatedBranch(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, "separatedBranch")) {
            return false
        }
        var result: Boolean
        val marker = enter_section_(builder)
        result = this.oneOrMoreSeparations.parse(builder, level + 1)
        if (result) {
            result = this.indentation == IndentationHelper.getIndentationOfPreviousToken(builder)
        }
        result = result && this.branch.parse(builder, level + 1)
        exit_section_(builder, marker, null, result)
        return result
    }
}