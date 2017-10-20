package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor

class ElmExposedUnion(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitExposedUnion(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val exposedUnionConstructors: ElmExposedUnionConstructors?
        get() = findChildByClass(ElmExposedUnionConstructors::class.java)

    val upperCaseId: ElmUpperCaseId
        get() = findNotNullChildByClass(ElmUpperCaseId::class.java)

}
