package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmList(node: ASTNode) : ElmPsiElementImpl(node) {

    val expressionList: List<ElmExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpression::class.java)

}
