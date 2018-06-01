package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmExpression(node: ASTNode) : ElmPsiElementImpl(node) {

    val listOfOperandsList: List<ElmListOfOperands>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmListOfOperands::class.java)

}
