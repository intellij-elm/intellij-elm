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
 *  - `(\x -> x) 1`
 *  - `(a << b) c
 */
class ElmFunctionCallExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmExpressionTag, ElmOperandTag {

    /** The function being called */
    val target: ElmFunctionCallTargetTag get() = findNotNullChildByClass(ElmFunctionCallTargetTag::class.java)

    /** The arguments to the function. This will always have at least one element */
    val arguments: Sequence<ElmAtomTag> get() = directChildren.filterIsInstance<ElmAtomTag>().drop(1)
}
