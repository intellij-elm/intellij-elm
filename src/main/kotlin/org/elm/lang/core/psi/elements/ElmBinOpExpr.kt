package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren


/**
 * One or more binary operator expressions
 */
class ElmBinOpExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmExpressionTag {
    val operators: Sequence<ElmOperator> get() = directChildren.filterIsInstance<ElmOperator>()
    val atoms: Sequence<ElmAtomTag> get() = directChildren.filterIsInstance<ElmAtomTag>()
}
