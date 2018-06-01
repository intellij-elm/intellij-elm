package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


class ElmInnerTypeAnnotation(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * The name of the declaration which this annotates
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)


    /**
     * The type signature.
     *
     * In a well-formed program, this will be non-null
     */
    val typeRef: ElmTypeRef?
        get() = findChildByClass(ElmTypeRef::class.java)

}
