
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmRecord(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitRecord(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val fieldList: List<ElmField>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmField::class.java)

    val lowerCaseId: ElmLowerCaseId?
        get() = findChildByClass(ElmLowerCaseId::class.java)

}
