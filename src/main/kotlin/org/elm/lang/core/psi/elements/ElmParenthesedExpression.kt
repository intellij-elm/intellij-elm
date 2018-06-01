package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmParenthesedExpression(node: ASTNode) : ElmPsiElementImpl(node) {

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

}
