package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.resolve.scope.ExpressionScope


/**
 * Accessing one or more fields on a base record.
 *
 * e.g. `model.currentUser.name`
 */
class ElmFieldAccessExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmOperandTag, ElmFunctionCallTargetTag {

    val lowerCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(LOWER_CASE_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifierList.first()

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
