package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand


class ElmNonEmptyTuple(node: ASTNode) : ElmPsiElementImpl(node), ElmOperand {

    val expressionList: List<ElmExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpression::class.java)

}
