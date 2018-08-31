package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmExpressionPartTag
import org.elm.lang.core.psi.ElmOperandTag


/**
 * A function call expression.
 *
 * e.g. `toString 1`, `(+) 1 2`, or `List.map toString [1, 2]`
 */
class ElmFunctionCall(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {
    /** The function being called, if it's not an operator */
    val function: ElmValueExpr? get() = findChildByClass(ElmValueExpr::class.java)

    /** The function being called, if it is an operator */
    val operator: ElmOperatorAsFunction? get() = findChildByClass(ElmOperatorAsFunction::class.java)

    /** The arguments to the function. This will always have at least one element */
    val operands: Sequence<ElmOperandTag> get() = directChildren.filterIsInstance<ElmOperandTag>()
}
