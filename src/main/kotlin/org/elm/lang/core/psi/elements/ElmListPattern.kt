package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmConsPatternChildTag
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmPatternChildTag


/**
 * A list pattern.
 *
 * e.g. `[a, (b, c)]`
 */
class ElmListPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag, ElmFunctionParamTag,
        ElmPatternChildTag {
    /** The patterns that are part of the list. May be empty. */
    val parts: Sequence<ElmConsPatternChildTag> get() = directChildren.filterIsInstance<ElmConsPatternChildTag>()
}
