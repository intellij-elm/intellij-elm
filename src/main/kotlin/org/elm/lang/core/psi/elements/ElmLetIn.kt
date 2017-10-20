
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmLetIn(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitLetIn(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

    val innerTypeAnnotationList: List<ElmInnerTypeAnnotation>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerTypeAnnotation::class.java)

    val innerValueDeclarationList: List<ElmInnerValueDeclaration>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerValueDeclaration::class.java)

}
