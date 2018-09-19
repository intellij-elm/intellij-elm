package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * A function call expression.
 *
 * e.g.
 *  - `toString 1`
 *  - `(+) 1 2`
 *  - `List.map toString [1, 2]`
 *  - `record.functionInField ()`
 */
class ElmFunctionCall(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {
    /** The function or operator being called */
    val target: ElmFunctionCallTarget get() = findNotNullChildByClass(ElmFunctionCallTarget::class.java)

    /** The arguments to the function. This will always have at least one element */
    val operands: Sequence<ElmOperandTag> get() = directChildren.filterIsInstance<ElmOperandTag>()
}
