package org.elm.lang.core.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmTypes.ALIAS
import org.elm.lang.core.psi.ElmTypes.AS
import org.elm.lang.core.psi.ElmTypes.CASE
import org.elm.lang.core.psi.ElmTypes.CHAR_LITERAL
import org.elm.lang.core.psi.ElmTypes.COMMENT_CONTENT
import org.elm.lang.core.psi.ElmTypes.ELSE
import org.elm.lang.core.psi.ElmTypes.END_COMMENT
import org.elm.lang.core.psi.ElmTypes.EXPOSING
import org.elm.lang.core.psi.ElmTypes.IF
import org.elm.lang.core.psi.ElmTypes.IMPORT
import org.elm.lang.core.psi.ElmTypes.IN
import org.elm.lang.core.psi.ElmTypes.INFIX
import org.elm.lang.core.psi.ElmTypes.INFIXL
import org.elm.lang.core.psi.ElmTypes.INFIXR
import org.elm.lang.core.psi.ElmTypes.LET
import org.elm.lang.core.psi.ElmTypes.LINE_COMMENT
import org.elm.lang.core.psi.ElmTypes.LIST_CONSTRUCTOR
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.MODULE
import org.elm.lang.core.psi.ElmTypes.NUMBER_LITERAL
import org.elm.lang.core.psi.ElmTypes.OF
import org.elm.lang.core.psi.ElmTypes.OPERATOR
import org.elm.lang.core.psi.ElmTypes.PORT
import org.elm.lang.core.psi.ElmTypes.RESERVED
import org.elm.lang.core.psi.ElmTypes.START_COMMENT
import org.elm.lang.core.psi.ElmTypes.START_DOC_COMMENT
import org.elm.lang.core.psi.ElmTypes.STRING_LITERAL
import org.elm.lang.core.psi.ElmTypes.THEN
import org.elm.lang.core.psi.ElmTypes.TYPE
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.WHERE


/** type of PSI tree leaf nodes (tokens) */
class ElmTokenType(debugName: String) : IElementType(debugName, ElmLanguage)


fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val ELM_COMMENTS = tokenSetOf(
        LINE_COMMENT,
        START_COMMENT,
        START_DOC_COMMENT,
        COMMENT_CONTENT,
        END_COMMENT
)

val ELM_KEYWORDS = tokenSetOf(
        WHERE,
        MODULE,
        IMPORT,
        AS,
        EXPOSING,
        IF,
        THEN,
        ELSE,
        CASE,
        OF,
        LET,
        IN,
        TYPE,
        ALIAS,
        PORT,
        INFIXL,
        INFIX,
        INFIXR,
        RESERVED
)

val ELM_OPERATORS = tokenSetOf(
        OPERATOR,
        LIST_CONSTRUCTOR
)

val ELM_IDENTIFIERS = tokenSetOf(
        UPPER_CASE_IDENTIFIER,
        LOWER_CASE_IDENTIFIER
)

val ELM_LITERALS = tokenSetOf(
        STRING_LITERAL,
        CHAR_LITERAL,
        NUMBER_LITERAL
)