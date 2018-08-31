package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmExpressionPartTag
import org.elm.lang.core.psi.ElmOperandTag


/**
 * A negated expression (one with a leading `-` operator)
 *
 * e.g. `-3`
 */
class ElmNegateExpresssion(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {
    /** The negated expression */
    val expression: ElmExpression = findNotNullChildByClass(ElmExpression::class.java)
}
