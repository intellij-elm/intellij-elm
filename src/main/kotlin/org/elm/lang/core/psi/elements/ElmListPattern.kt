package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmConsPatternChildTag


/**
 * A list pattern.
 *
 * e.g. `[a, (b, c)]`
 */
class ElmListPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag {
    /** The patterns that are part of the list. May be empty. */
    val parts: Sequence<ElmConsPatternChildTag> get() = directChildren.filterIsInstance<ElmConsPatternChildTag>()
}
