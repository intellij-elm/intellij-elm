package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand


class ElmIfElse(node: ASTNode) : ElmPsiElementImpl(node), ElmOperand {

    /**
     * Will contain at least 2 expressions in a well-formed program.
     */
    val expressionList: List<ElmExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpression::class.java)

}
