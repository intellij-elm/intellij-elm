package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


class ElmExpression(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFunctionCallTargetTag {

    /** The atoms and operators in this expression */
    val parts: Sequence<ElmExpressionPartTag> get() = directChildren.filterIsInstance<ElmExpressionPartTag>()
}
