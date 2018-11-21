package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.resolve.scope.ExpressionScope

/**
 * Base expression which can start a field access chain
 */
class ElmFieldAccessStart(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    // at least one of the 3 will be non-null

    val recordLiteral: ElmRecord?
        get() = findChildByClass(ElmRecord::class.java)

    val parenthesizedExpression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)

    val lowerCaseIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)


    // PSI REFERENCE

    // TODO [kl] provide dummy/null reference in record literal and parenthesized-expr cases

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier!!

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            BaseRecordReference(this)
}


class BaseRecordReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    /* TODO [kl] a more correct implementation would filter the visible values
       to those that we know are actually records. But we haven't implemented
       any kind of type system in the plugin yet. */
    override fun getVariants(): Array<ElmNamedElement> =
            ExpressionScope(element).getVisibleValues()
                    .toTypedArray()
}