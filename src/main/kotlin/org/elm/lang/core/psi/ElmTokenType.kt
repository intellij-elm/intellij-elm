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
        BLOCK_COMMENT,
        DOC_COMMENT
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

/**
 * The tokens corresponding to top-level code declarations.
 *
 * NOTE: this **excludes** module declarations and import statements
 */
val ELM_TOP_LEVEL_DECLARATIONS = tokenSetOf(
        TYPE_DECLARATION, TYPE_ALIAS_DECLARATION, VALUE_DECLARATION,
        TYPE_ANNOTATION, PORT_ANNOTATION
)

/** the virtual tokens which can be synthesized by [ElmLayoutLexer] */
val ELM_VIRTUAL_TOKENS = tokenSetOf(
        VIRTUAL_OPEN_SECTION,
        VIRTUAL_END_SECTION,
        VIRTUAL_END_DECL
)
