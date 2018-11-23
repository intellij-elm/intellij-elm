package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.childOfType


/**
 * A parenthesized expression with an accessor.
 *
 * e.g. `(fn arg).foo.bar`
 */
class ElmExpressionWithAccessorExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionCallTargetTag, ElmAtomTag {
    val expression: ElmExpression get() = childOfType()!!
    val accessor: ElmExpressionAccessor get() = childOfType()!!
}
