package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPatternChildTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren


/**
 * A list cons pattern.
 *
 * e.g. `1 :: [2, 3]`
 */
class ElmConsPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmPatternChildTag {
    /**
     * The patterns that are consed together.
     *
     * Note that an [ElmConsPattern] cannot be one of the parts, although it may contain one nested in a [ElmPattern].
     *
     * e.g. the pattern `(a :: [2]) :: [[3]]` will have parts `(a :: [2])` and `[[3]]`, with the first being an
     *   [ElmPattern].
     */
    val parts: Sequence<ElmPatternChildTag> get() = directChildren.filterIsInstance<ElmPatternChildTag>()
}
