package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.tags.ElmConsPatternChildTag
import org.elm.lang.core.psi.tags.ElmOperandTag


/**
 * A literal string, char, or number.
 *
 * e.g. `1`, `'c'`, or `"string"`
 */
class ElmLiteral(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmConsPatternChildTag {
    /**
     * The literal element. It will have type [STRING_LITERAL], [CHAR_LITERAL], or [NUMBER_LITERAL].
     */
    val element: PsiElement get() = firstChild

    val isNumber: Boolean get() = element.elementType == NUMBER_LITERAL
    val isChar: Boolean get() = element.elementType == CHAR_LITERAL
    val isString: Boolean get() = element.elementType == STRING_LITERAL
}
