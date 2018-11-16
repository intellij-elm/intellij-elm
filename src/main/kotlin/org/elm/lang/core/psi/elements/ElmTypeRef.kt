package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmParametricTypeRefParameterTag
import org.elm.lang.core.psi.ElmTypeRefSegmentTag
import org.elm.lang.core.psi.ElmUnionMemberParameterTag


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
class ElmTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionMemberParameterTag, ElmParametricTypeRefParameterTag, ElmTypeRefSegmentTag {

    /**
     * All segments of the type annotation.
     *
     * The segments will be in source order. If this element is not a function, there will be one segment in
     * well-formed programs. For functions, there will be one segment per function argument, plus the return type.
     *
     * e.g. `Int` and `String` in `Int -> String`
     */
    val allSegments: Sequence<ElmTypeRefSegmentTag>
        get() = directChildren.filterIsInstance<ElmTypeRefSegmentTag>()
}
