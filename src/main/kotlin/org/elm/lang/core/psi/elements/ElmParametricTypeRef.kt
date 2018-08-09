package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmParametricTypeRefParameter
import org.elm.lang.core.psi.tags.ElmTypeRefParameter
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
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
class ElmParametricTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmTypeRefParameter {

    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    /**
     * All parameters of the type.
     *
     * The elements will be in source order, and will be any of the following types:
     *
     * [ElmUpperPathTypeRef], [ElmTypeVariableRef], [ElmRecordType], [ElmTupleType], [ElmTypeRef]
     */
    val allParameters: Sequence<ElmParametricTypeRefParameter>
        get() = directChildren.filterIsInstance<ElmParametricTypeRefParameter>()

    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> {
        return if (upperCaseQID.upperCaseIdentifierList.size > 1) {
            arrayOf(QualifiedTypeReference(this, upperCaseQID),
                    QualifiedModuleNameReference(this, upperCaseQID))
        } else {
            arrayOf(SimpleTypeReference(this))
        }
    }
}
