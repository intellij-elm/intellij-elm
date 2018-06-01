package org.elm.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.psi.ElmTypes.*


private val bracePairs = arrayOf(
        BracePair(LEFT_BRACE, RIGHT_BRACE, true),
        BracePair(LEFT_PARENTHESIS, RIGHT_PARENTHESIS, false),
        BracePair(LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET, true))


class ElmPairedBraceMatcher : PairedBraceMatcher {

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) =
    // TODO [kl] re-visit this later. the default is adequate for now.
            openingBraceOffset


    override fun getPairs() =
            bracePairs


    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) =
            when (contextType) {
                null -> true
                TAB -> true
                NEWLINE -> true
                WHITE_SPACE -> true
                else -> false
            }
}