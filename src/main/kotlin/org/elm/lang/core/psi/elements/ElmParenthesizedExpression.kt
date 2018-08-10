package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperandTag


class ElmParenthesizedExpression(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)
}
