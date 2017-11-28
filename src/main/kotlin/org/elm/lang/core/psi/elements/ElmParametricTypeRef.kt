package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.QualifiedModuleNameReference
import org.elm.lang.core.resolve.reference.QualifiedTypeReference
import org.elm.lang.core.resolve.reference.SimpleTypeReference


/**
 * A type which is parameterized by one or more type parameters.
 *
 * The type parameters can be either a type variable (all lowercase) or an actual type
 * (initial uppercase).
 *
 * The type itself as well as the actual type args may be qualified by the module/alias
 * where the type is defined.
 *
 * e.g.
 * - `List a`
 * - `List String`
 * - `Task Http.Error String`
 */
class ElmParametricTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)


    // TODO [kl] cleanup the Psi tree so that this isn't such a mess
    val upperPathTypeRefList: List<ElmUpperPathTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperPathTypeRef::class.java)

    val typeVariableRefList: List<ElmTypeVariableRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeVariableRef::class.java)

    val recordTypeList: List<ElmRecordType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecordType::class.java)

    val tupleTypeList: List<ElmTupleType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTupleType::class.java)

    val typeRefList: List<ElmTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeRef::class.java)



    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            getReferences().first()

    override fun getReferences(): Array<PsiReference> {
        return if (upperCaseQID.upperCaseIdentifierList.size > 1)
            arrayOf(QualifiedTypeReference(this, upperCaseQID),
                    QualifiedModuleNameReference(this, upperCaseQID))
        else
            arrayOf(SimpleTypeReference(this))
    }
}