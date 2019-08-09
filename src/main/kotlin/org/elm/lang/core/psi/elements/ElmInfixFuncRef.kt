package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference

/**
 * A reference to the function that implements an infix operator.
 */
class ElmInfixFuncRef(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference =
            LocalTopLevelValueReference(this)
}