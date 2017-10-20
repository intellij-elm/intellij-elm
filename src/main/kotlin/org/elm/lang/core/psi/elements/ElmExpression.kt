
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmExpression(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitExpression(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val listOfOperandsList: List<ElmListOfOperands>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmListOfOperands::class.java)

}
