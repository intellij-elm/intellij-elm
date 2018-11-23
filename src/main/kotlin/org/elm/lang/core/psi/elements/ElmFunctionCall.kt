package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmAtomTag
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
class ElmFunctionCall(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag {
    /** The function or operator being called */
    val target: ElmFunctionCallTargetTag get() = findNotNullChildByClass(ElmFunctionCallTargetTag::class.java)

    /** The arguments to the function. This will always have at least one element */
    val arguments: Sequence<ElmAtomTag> get() = directChildren.filterIsInstance<ElmAtomTag>().drop(1)
}
