
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmVisitor
import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmNamedElementImpl


class ElmTypeDeclaration(node: ASTNode) : ElmNamedElementImpl(node), ElmNameIdentifierOwner {

    fun accept(visitor: ElmVisitor) {
        visitor.visitTypeDeclaration(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val lowerCaseIdList: List<ElmLowerCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerCaseId::class.java)

    val unionMemberList: List<ElmUnionMember>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnionMember::class.java)

    val upperCaseId: ElmUpperCaseId
        get() = findNotNullChildByClass(ElmUpperCaseId::class.java)

}
