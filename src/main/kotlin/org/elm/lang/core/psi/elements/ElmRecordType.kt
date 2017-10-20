
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmRecordType(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitRecordType(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val fieldTypeList: List<ElmFieldType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmFieldType::class.java)

    val lowerCaseId: ElmLowerCaseId?
        get() = findChildByClass(ElmLowerCaseId::class.java)

}
