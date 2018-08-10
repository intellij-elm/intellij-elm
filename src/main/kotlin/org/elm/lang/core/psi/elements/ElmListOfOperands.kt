package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmExpressionPartTag
import org.elm.lang.core.psi.tags.ElmOperandTag


class ElmListOfOperands(node: ASTNode) : ElmPsiElementImpl(node), ElmExpressionPartTag {
    /** The operands in this list */
    val operands: Sequence<ElmOperandTag> get() = directChildren.filterIsInstance<ElmOperandTag>()
}
