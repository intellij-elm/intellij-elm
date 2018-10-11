package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * A list pattern.
 *
 * e.g. `[a, (b, c)]`
 */
class ElmListPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag, ElmFunctionParamTag,
        ElmPatternChildTag, ElmUnionPatternChildTag {
    /** The patterns that are part of the list. May be empty. */
    val parts: Sequence<ElmConsPatternChildTag> get() = directChildren.filterIsInstance<ElmConsPatternChildTag>()
}
