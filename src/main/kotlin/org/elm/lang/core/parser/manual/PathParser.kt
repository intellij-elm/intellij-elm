package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.tree.IElementType

import com.intellij.lang.parser.GeneratedParserUtilBase.*
import org.elm.lang.core.psi.ElmTypes.DOT

class PathParser(private val sectionType: IElementType,
                 private var memberType: IElementType?,
                 private var alternativeMemberType: IElementType?,
                 private var memberParser: Parser?, // TODO [kl] does this really need to be optional?
                 private var alternativeMemberParser: Parser?
) : GeneratedParserUtilBase.Parser {

    override fun parse(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, this.getGuardText()))
            return false

        if (!nextTokenIs(builder, this.memberType))
            return false

        var result: Boolean
        val isContinued = builder.rawLookup(1) === DOT
        val marker = enter_section_(builder)
        result = this.memberParser!!.parse(builder, level + 1) //consumeToken(builder, this.memberType);
        result = result && (!isContinued || listOfMembers(builder, level + 1))
        exit_section_(builder, marker, this.sectionType, result)
        return result
    }

    private fun listOfMembers(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, this.getGuardText(1)))
            return false

        var c = current_position_(builder)
        var isContinued: Boolean
        while (true) {
            if (builder.rawLookup(0) !== DOT)
                break

            if (builder.rawLookup(1) !== this.memberType && !trySwitchingMemberType(builder))
                break

            isContinued = builder.rawLookup(2) === DOT
            if (!dotMember(builder, level + 1))
                break

            if (!empty_element_parsed_guard_(builder, this.getGuardText(1), c))
                break

            c = current_position_(builder)
            if (!isContinued)
                break
        }
        return true
    }

    private fun trySwitchingMemberType(builder: PsiBuilder): Boolean {
        if (this.alternativeMemberParser != null && builder.rawLookup(1) === this.alternativeMemberType) {
            this.memberType = this.alternativeMemberType
            this.alternativeMemberType = null
            this.memberParser = this.alternativeMemberParser
            this.alternativeMemberParser = null
            return true
        }
        return false
    }

    private fun dotMember(builder: PsiBuilder, level: Int): Boolean {
        if (!recursion_guard_(builder, level, this.getGuardText(1, 0)))
            return false

        var result: Boolean
        val m = enter_section_(builder)
        result = consumeToken(builder, DOT)
        result = result && this.memberParser!!.parse(builder, level + 1)
        exit_section_(builder, m, null, result)
        return result
    }

    private fun getGuardText(vararg numbers: Int): String {
        val result = StringBuilder(this.sectionType.toString().toLowerCase())
        for (number in numbers) {
            result.append('_')
            result.append(number)
        }
        return result.toString()
    }
}
