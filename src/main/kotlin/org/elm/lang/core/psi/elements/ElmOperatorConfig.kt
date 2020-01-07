package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.tokenSetOf
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.resolve.scope.ModuleScope

// TODO [drop 0.18] delete this entire file

/**
 * A top-level declaration that describes the associativity and the precedence
 * of a binary operator.
 *
 * For example, `infixl 0 ++` means that the operator `++` has left associativity
 * at precedence level of 0.
 *
 */
class ElmOperatorConfig(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val associativityKeyword: PsiElement
        get() = findNotNullChildByType<PsiElement>(tokenSetOf(INFIX, INFIXL, INFIXR))

    val precedence: PsiElement
        get() = findNotNullChildByType(NUMBER_LITERAL)

    val operatorIdentifier: PsiElement
        get() = findNotNullChildByType(OPERATOR_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = operatorIdentifier

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            LocalOperatorReference(this)
}


/**
 * Reference to a binary operator local to this file
 */
class LocalOperatorReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        return ModuleScope.getDeclaredValuesByName(element.elmFile)[element.referenceName]
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] filter the variants to just include binary operators
        return ModuleScope.getDeclaredValues(element.elmFile).toTypedArray()
    }
}
