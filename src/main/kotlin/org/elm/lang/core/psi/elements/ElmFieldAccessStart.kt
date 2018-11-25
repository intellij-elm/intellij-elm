package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFieldAccessPartTag
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmDummyReference
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.resolve.scope.ExpressionScope

/**
 * Begins a chain of field access
 *
 * e.g. `model` in `model.currentUser.name`
 */
class ElmFieldAccessStart(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmFieldAccessPartTag {

    val lowerCaseIdentifier: PsiElement?
        get() = findChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    val recordExpr: ElmRecordExpr?
        get() = findChildByClass(ElmRecordExpr::class.java)

    val parenExpr: ElmParenthesizedExpr?
        get() = findChildByClass(ElmParenthesizedExpr::class.java)

    /*
        NOTE: due to how we've defined ElmReferenceElement, a Psi element
              cannot conditionally return a reference. But in this case
              we do want to do it conditionally. So in the situations where
              a reference is not applicable, we will return a dummy reference
              that always fails. This will need to be special-cased elsewhere.
     */

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier ?: this

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            if (lowerCaseIdentifier == null) {
                ElmDummyReference(this)
            } else {
                BaseRecordReference(this)
            }
}

class BaseRecordReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> =
    // TODO [kl] restrict this to just things that have type Record
            ExpressionScope(element).getVisibleValues()
                    .toTypedArray()
}