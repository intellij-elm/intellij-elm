
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference


/**
 * Either [lowerCaseIdentifier] or [operatorAsFunction] is non-null
 */
class ElmTypeAnnotation(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val lowerCaseIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)

    val operatorAsFunction: ElmOperatorAsFunction?
        get() = findChildByClass(ElmOperatorAsFunction::class.java)

    val typeRef: ElmTypeRef
        get() = findNotNullChildByClass(ElmTypeRef::class.java)

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier
                    ?: operatorAsFunction?.operator
                    ?: throw RuntimeException("cannot determine type annotations's ref name element")

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): PsiReference {
        return LocalTopLevelValueReference(this)
    }
}
