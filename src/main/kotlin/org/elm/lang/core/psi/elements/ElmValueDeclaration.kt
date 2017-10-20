
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmValueDeclaration(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitValueDeclaration(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

    val functionDeclarationLeft: ElmFunctionDeclarationLeft?
        get() = findChildByClass(ElmFunctionDeclarationLeft::class.java)

    val operatorDeclarationLeft: ElmOperatorDeclarationLeft?
        get() = findChildByClass(ElmOperatorDeclarationLeft::class.java)

    val pattern: ElmPattern?
        get() = findChildByClass(ElmPattern::class.java)

}
