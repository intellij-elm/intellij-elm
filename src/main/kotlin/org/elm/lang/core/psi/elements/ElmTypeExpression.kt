package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * A type expression.
 *
 * e.g.
 *
 *  - `Float`
 *  - `Maybe a`
 *  - `Int -> String`
 *  - `a -> (a -> {a: String})`
 */
class ElmTypeExpression(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionVariantParameterTag,
        ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    /**
     * All segments of the type expression.
     *
     * The segments will be in source order. If this element is not a function, there will be one segment in
     * well-formed programs. For functions, there will be one segment per function argument, plus the return type.
     *
     * e.g. `Int` and `String` in `Int -> String`
     */
    val allSegments: Sequence<ElmTypeExpressionSegmentTag>
        get() = directChildren.filterIsInstance<ElmTypeExpressionSegmentTag>()
}
