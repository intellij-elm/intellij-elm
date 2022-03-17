package org.elm.ide.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.elm.ide.color.ElmColor
import org.elm.lang.core.lexer.ElmIncrementalLexer
import org.elm.lang.core.psi.ELM_KEYWORDS
import org.elm.lang.core.psi.ELM_OPERATORS
import org.elm.lang.core.psi.ElmTypes.*


class ElmSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() =
            ElmIncrementalLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
            pack(map(tokenType)?.textAttributesKey)

    companion object {
        fun map(tokenType: IElementType?): ElmColor? =
                when (tokenType) {
                    REGULAR_STRING_PART, OPEN_QUOTE, CLOSE_QUOTE, OPEN_CHAR, CLOSE_CHAR -> ElmColor.STRING
                    STRING_ESCAPE -> ElmColor.VALID_STRING_ESCAPE
                    INVALID_STRING_ESCAPE -> ElmColor.INVALID_STRING_ESCAPE
                    LEFT_PARENTHESIS, RIGHT_PARENTHESIS -> ElmColor.PARENTHESIS
                    LEFT_BRACE, RIGHT_BRACE -> ElmColor.BRACES
                    LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET -> ElmColor.BRACKETS
                    ARROW -> ElmColor.ARROW
                    EQ -> ElmColor.EQ
                    COMMA -> ElmColor.COMMA
                    DOT -> ElmColor.DOT
                    NUMBER_LITERAL -> ElmColor.NUMBER
                    PIPE -> ElmColor.PIPE
                    LINE_COMMENT -> ElmColor.LINE_COMMENT
                    BLOCK_COMMENT -> ElmColor.BLOCK_COMMENT
                    DOC_COMMENT -> ElmColor.DOC_COMMENT
                    in ELM_KEYWORDS -> ElmColor.KEYWORD
                    in ELM_OPERATORS -> ElmColor.OPERATOR
                    TokenType.BAD_CHARACTER -> ElmColor.BAD_CHAR
                    else -> null
                }
    }
}
