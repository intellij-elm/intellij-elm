package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmExpressionPart


class ElmExpression(node: ASTNode) : ElmPsiElementImpl(node) {

    /** The operands and operators in this expression */
    val parts: Sequence<ElmExpressionPart> get() = directChildren.filterIsInstance<ElmExpressionPart>()
}
