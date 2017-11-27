
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


class ElmInnerTypeAnnotation(node: ASTNode) : ElmPsiElementImpl(node) {

    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    val typeRef: ElmTypeRef
        get() = findNotNullChildByClass(ElmTypeRef::class.java)

}
