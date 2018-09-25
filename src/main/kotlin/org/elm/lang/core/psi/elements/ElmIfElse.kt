package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmOperandTag


class ElmIfElse(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {

    /**
     * Will contain 3 expressions in a well-formed program: the condition, the body of the if, and the body of the else
     */
    val expressionList: List<ElmExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpression::class.java)

}
