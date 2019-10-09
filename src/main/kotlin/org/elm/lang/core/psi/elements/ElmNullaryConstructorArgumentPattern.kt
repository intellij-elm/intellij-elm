package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmUnionPatternChildTag
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ModuleNameQualifierReference
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.SimpleUnionConstructorReference


/**
 * A pattern that matches a zero-parameter variant constructor when used as the argument to another
 * variant constructor in a pattern.
 *
 * e.g. `Nothing` in `Just Nothing` when used as a function parameter or case pattern.
 */
class ElmNullaryConstructorArgumentPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmUnionPatternChildTag {

    /** The variant constructor */
    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> =
            if (upperCaseQID.isQualified)
                arrayOf(QualifiedConstructorReference(this, upperCaseQID),
                        ModuleNameQualifierReference(this, upperCaseQID, upperCaseQID.qualifierPrefix))
            else
                arrayOf(SimpleUnionConstructorReference(this))

}
