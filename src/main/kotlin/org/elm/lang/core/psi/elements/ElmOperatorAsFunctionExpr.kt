package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.SimpleOperatorReference


/**
 * A binary operator treated as a function such that it can be passed as an argument
 * to another function.
 *
 * e.g. the `(+)` in the expression `foldl (+) 0`
 */
class ElmOperatorAsFunctionExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmAtomTag, ElmFunctionCallTargetTag {

    val operator: PsiElement
        get() = findNotNullChildByType(OPERATOR_IDENTIFIER)

    val leftParen: PsiElement
        get() = findNotNullChildByType(LEFT_PARENTHESIS)

    val rightParen: PsiElement
        get() = findNotNullChildByType(RIGHT_PARENTHESIS)


    override val referenceNameElement: PsiElement
        get() = operator

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            SimpleOperatorReference(this)
}
