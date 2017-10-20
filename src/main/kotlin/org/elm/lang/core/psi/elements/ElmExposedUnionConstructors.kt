package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmExposedUnionConstructors(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitExposedUnionConstructors(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val upperCaseIdList: List<ElmUpperCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperCaseId::class.java)

}
