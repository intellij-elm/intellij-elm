package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmPatternChild


class ElmPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmPatternChild {

    /**
     * The actual type of this pattern.
     *
     * If this patten is wrapped in parenthesis, the child will be another [ElmPattern]
     */
    val child: ElmPatternChild get() = findNotNullChildByClass(ElmPatternChild::class.java)
}
