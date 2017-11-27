
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase


class ElmTypeDeclaration(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.UPPER) {

    val identifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerTypeName::class.java)

    val unionMemberList: List<ElmUnionMember>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnionMember::class.java)

}
