package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmConsPatternChildTag
import org.elm.lang.core.psi.tags.ElmPatternChildTag


class ElmPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag {

    /**
     * The actual type of this pattern.
     *
     * If this patten is wrapped in parenthesis, the child will be another [ElmPattern]
     */
    val child: ElmPatternChildTag
        get() = findNotNullChildByClass(ElmPatternChildTag::class.java)

    val patternAs: ElmPatternAs?
        get() = findChildByClass(ElmPatternAs::class.java)
}
