
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


class ElmModuleDeclaration(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitModuleDeclaration(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val upperCasePath: ElmUpperCasePath
        get() = findNotNullChildByClass(ElmUpperCasePath::class.java)

    val exposedUnionList: List<ElmExposedUnion>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExposedUnion::class.java)

    val lowerCaseIdList: List<ElmLowerCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerCaseId::class.java)

    val operatorAsFunctionList: List<ElmOperatorAsFunction>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmOperatorAsFunction::class.java)

    val effectModuleDetailRecord: ElmRecord?
        get() = findChildByClass(ElmRecord::class.java)

}
