
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmTypeAliasDeclaration(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitTypeAliasDeclaration(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val lowerCaseIdList: List<ElmLowerCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerCaseId::class.java)

    val typeDefinition: ElmTypeDefinition
        get() = findNotNullChildByClass(ElmTypeDefinition::class.java)

    val upperCaseId: ElmUpperCaseId
        get() = findNotNullChildByClass(ElmUpperCaseId::class.java)

}
