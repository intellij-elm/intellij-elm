package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ModuleNameQualifierReference
import org.elm.lang.core.resolve.reference.QualifiedTypeReference
import org.elm.lang.core.resolve.reference.SimpleTypeReference


/**
 * A type expression which references an [ElmTypeAliasDeclaration] or an [ElmTypeDeclaration].
 *
 * It may have one or more type arguments, which are each a type expression.
 *
 * e.g.
 * - `String`
 * - `List a`
 * - `List { x : Int }`
 * - `Task.Task Http.Error String`
 */
class ElmTypeRef(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmTypeExpressionSegmentTag,
        ElmTypeRefArgumentTag, ElmUnionVariantParameterTag {

    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    /**
     * All arguments to the type, if there are any.
     *
     * The elements will be in source order.
     */
    val allArguments: Sequence<ElmTypeRefArgumentTag>
        get() = directChildren.filterIsInstance<ElmTypeRefArgumentTag>()

    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> {
        return if (upperCaseQID.upperCaseIdentifierList.size > 1) {
            arrayOf(QualifiedTypeReference(this, upperCaseQID),
                    ModuleNameQualifierReference(this, upperCaseQID))
        } else {
            arrayOf(SimpleTypeReference(this))
        }
    }
}
