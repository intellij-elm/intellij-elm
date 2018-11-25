package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * A negated expression (one with a leading `-` operator)
 *
 * e.g. `-3`
 */
class ElmNegateExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag {
    /** The negated expression. In a well-formed program, this will never be null. */
    val expression: ElmExpressionTag? get() = findChildByClass(ElmExpressionTag::class.java)
}
