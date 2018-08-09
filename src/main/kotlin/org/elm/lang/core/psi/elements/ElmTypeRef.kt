package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmParametricTypeRefParameter
import org.elm.lang.core.psi.tags.ElmTypeRefParameter
import org.elm.lang.core.psi.tags.ElmUnionMemberParameter


/**
 * A type reference.
 *
 * e.g.
 *
 *  - `Float`
 *  - `Maybe a`
 *  - `Int -> String`
 *  - `a -> (a -> {a: String})`
 */
class ElmTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionMemberParameter, ElmParametricTypeRefParameter {

    /**
     * All parameters of the type annotation.
     *
     * The elements will be in source order. If the reference is not a function, there will be one parameter in
     * well-formed programs. For functions, there will be one parameter per function argument, plus the return type.
     */
    val allParameters: Sequence<ElmTypeRefParameter>
        get() = directChildren.filterIsInstance<ElmTypeRefParameter>()
}
