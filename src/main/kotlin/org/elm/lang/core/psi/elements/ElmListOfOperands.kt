package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmExpressionPart
import org.elm.lang.core.psi.tags.ElmOperand


class ElmListOfOperands(node: ASTNode) : ElmPsiElementImpl(node), ElmExpressionPart {
    /** The operands in this list */
    val operands: Sequence<ElmOperand> get() = directChildren.filterIsInstance<ElmOperand>()
}
