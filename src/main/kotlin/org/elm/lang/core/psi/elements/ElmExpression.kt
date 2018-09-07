package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmExpressionPartTag
import org.elm.lang.core.psi.ElmOperandTag


class ElmExpression(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {

    /** The operands and operators in this expression */
    val parts: Sequence<ElmExpressionPartTag> get() = directChildren.filterIsInstance<ElmExpressionPartTag>()
}
