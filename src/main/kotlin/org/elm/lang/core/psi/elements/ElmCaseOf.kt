package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor

class ElmCaseOf(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitCaseOf(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val branches: List<ElmCaseOfBranch>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmCaseOfBranch::class.java)

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

}
