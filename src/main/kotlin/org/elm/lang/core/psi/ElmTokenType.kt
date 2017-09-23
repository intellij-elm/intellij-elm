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
        START_COMMENT,
        START_DOC_COMMENT,
        END_COMMENT,
        COMMENT_CONTENT
)
