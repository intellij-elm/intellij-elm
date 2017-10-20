package org.elm.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor


abstract class ElmPsiElement(node: ASTNode) : ASTWrapperPsiElement(node) {

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            visitor.visitPsiElement(this)
        else
            super.accept(visitor)
    }

}
