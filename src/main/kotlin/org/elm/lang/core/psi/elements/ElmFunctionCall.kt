package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTarget
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren


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
class ElmFunctionCall(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {
    /** The function or operator being called */
    val target: ElmFunctionCallTarget get() = findNotNullChildByClass(ElmFunctionCallTarget::class.java)

    /** The arguments to the function. This will always have at least one element */
    val arguments: Sequence<ElmOperandTag> get() = directChildren.filterIsInstance<ElmOperandTag>().drop(1)
}
