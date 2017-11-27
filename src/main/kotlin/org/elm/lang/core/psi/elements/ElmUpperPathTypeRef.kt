package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.QualifiedTypeModuleNameReference
import org.elm.lang.core.resolve.reference.QualifiedTypeReference
import org.elm.lang.core.resolve.reference.SimpleTypeReference

/**
 * An upper-case path which references a type
 *
 * e.g. type Error = Network Http.Error
 *                           ^^^^^^^^^^
 *                           this
 */
class ElmUpperPathTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)



    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            getReferences().first()

    override fun getReferences(): Array<PsiReference> {
        return if (upperCaseQID.upperCaseIdentifierList.size > 1)
            arrayOf(QualifiedTypeReference(this, upperCaseQID),
                    QualifiedTypeModuleNameReference(this, upperCaseQID))
        else
            arrayOf(SimpleTypeReference(this))
    }
}
