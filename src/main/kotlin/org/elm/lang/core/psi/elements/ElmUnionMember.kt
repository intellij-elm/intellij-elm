
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmUnionMember(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitUnionMember(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val lowerCaseIdList: List<ElmLowerCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerCaseId::class.java)

    val recordTypeList: List<ElmRecordType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecordType::class.java)

    val tupleTypeList: List<ElmTupleType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTupleType::class.java)

    val typeDefinitionList: List<ElmTypeDefinition>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeDefinition::class.java)

    val upperCaseId: ElmUpperCaseId
        get() = findNotNullChildByClass(ElmUpperCaseId::class.java)

}
