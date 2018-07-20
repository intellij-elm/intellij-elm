package org.elm.lang.core.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmTypes.*


/** type of PSI tree leaf nodes (tokens) */
class ElmTokenType(debugName: String) : IElementType(debugName, ElmLanguage)


fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val ELM_COMMENTS = tokenSetOf(
        LINE_COMMENT,
        BLOCK_COMMENT
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
        INFIX,
        RESERVED
)

val ELM_OPERATORS = tokenSetOf(
        OPERATOR_IDENTIFIER
)

val ELM_IDENTIFIERS = tokenSetOf(
        UPPER_CASE_IDENTIFIER,
        LOWER_CASE_IDENTIFIER
)
