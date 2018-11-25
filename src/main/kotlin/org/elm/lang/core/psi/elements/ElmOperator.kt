package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmBinOpPartTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.SimpleOperatorReference


/**
 * A binary operator used in an expression
 *
 * e.g. `x + y`
 */
class ElmOperator(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmBinOpPartTag {

    val operator: PsiElement
        get() = findNotNullChildByType(OPERATOR_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = operator

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            SimpleOperatorReference(this)
}
