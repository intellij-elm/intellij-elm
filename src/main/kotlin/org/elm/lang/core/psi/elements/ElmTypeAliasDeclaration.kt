
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase


class ElmTypeAliasDeclaration(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.UPPER) {

    /** The new name (alias) which will hereafter refer to [typeRef] */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /** Zero-or-more type variables which may appear in [typeRef] */
    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerTypeName::class.java)

    /** The type which is being aliased */
    val typeRef: ElmTypeRef
        get() = findNotNullChildByClass(ElmTypeRef::class.java)

}
