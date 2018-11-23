package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * An Elm expression wrapped in parentheses
 *
 * e.g. `(42)`
 */
class ElmParenthesizedExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionCallTargetTag {
    /** In a well-formed program, this will never be null. */
    val expression: ElmExpression? get() = findChildByClass(ElmExpression::class.java)
}
