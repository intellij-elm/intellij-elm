package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ModuleNameQualifierReference
import org.elm.lang.core.resolve.reference.QualifiedTypeReference
import org.elm.lang.core.resolve.reference.SimpleTypeReference
import org.elm.lang.core.stubs.ElmPlaceholderStub


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
class ElmTypeRef : ElmStubbedElement<ElmPlaceholderStub>,
        ElmReferenceElement, ElmTypeExpressionSegmentTag, ElmTypeRefArgumentTag, ElmUnionVariantParameterTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val upperCaseQID: ElmUpperCaseQID
        get() = stubDirectChildrenOfType<ElmUpperCaseQID>().single()

    /**
     * All arguments to the type, if there are any.
     *
     * The elements will be in source order.
     */
    val allArguments: List<ElmTypeRefArgumentTag>
        get() = stubDirectChildrenOfType()

    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = upperCaseQID.refName // stub-safe

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> {
        val qualifierPrefix = upperCaseQID.qualifierPrefix // stub-safe

        return if (qualifierPrefix != "") {
            arrayOf(QualifiedTypeReference(this, qualifierPrefix),
                    ModuleNameQualifierReference(this, upperCaseQID, qualifierPrefix))
        } else {
            arrayOf(SimpleTypeReference(this))
        }
    }
}
