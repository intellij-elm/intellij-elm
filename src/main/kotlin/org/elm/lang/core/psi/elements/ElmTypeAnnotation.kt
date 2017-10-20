
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmTypeAnnotation(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitTypeAnnotation(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val lowerCaseId: ElmLowerCaseId?
        get() = findChildByClass(ElmLowerCaseId::class.java)

    val operatorAsFunction: ElmOperatorAsFunction?
        get() = findChildByClass(ElmOperatorAsFunction::class.java)

    val typeDefinition: ElmTypeDefinition
        get() = findNotNullChildByClass(ElmTypeDefinition::class.java)

}
