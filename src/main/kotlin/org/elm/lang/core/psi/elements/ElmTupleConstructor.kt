
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmTupleConstructor(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitTupleConstructor(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

}
