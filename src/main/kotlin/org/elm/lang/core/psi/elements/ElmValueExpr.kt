package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.elements.Flavor.BareConstructor
import org.elm.lang.core.psi.elements.Flavor.BareValue
import org.elm.lang.core.psi.elements.Flavor.QualifiedConstructor
import org.elm.lang.core.psi.elements.Flavor.QualifiedValue
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmQualifiedReferenceBase
import org.elm.lang.core.resolve.reference.LexicalValueReference
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.QualifiedValueReference
import org.elm.lang.core.resolve.reference.SimpleUnionOrRecordConstructorReference


enum class Flavor {
    QualifiedValue,
    QualifiedConstructor,
    BareValue,
    BareConstructor
}

class ElmValueExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val valueQID: ElmValueQID?
        get() = findChildByClass(ElmValueQID::class.java)

    val upperCaseQID: ElmUpperCaseQID?
        get() = findChildByClass(ElmUpperCaseQID::class.java)


    val flavor: Flavor
        get() = when {
            valueQID != null ->
                if (valueQID!!.isQualified)
                    QualifiedValue
                else
                    BareValue
            upperCaseQID != null ->
                if (upperCaseQID!!.isQualified)
                    QualifiedConstructor
                else
                    BareConstructor
            else -> error("one QID must be non-null")
        }

    /**
     * Return the name element of the principal reference (the type or value, not the module prefix)
     */
    override val referenceNameElement: PsiElement
        get() = when (flavor) {
            QualifiedValue ->           valueQID!!.lowerCaseIdentifier
            QualifiedConstructor ->     upperCaseQID!!.upperCaseIdentifierList.last()
            BareValue ->                valueQID!!.lowerCaseIdentifier
            BareConstructor ->          upperCaseQID!!.upperCaseIdentifierList.last()
        }

    /**
     * Return the name of the principal reference (the type or value, not the module prefix)
     */
    override val referenceName: String
        get() = referenceNameElement.text


    override fun getReference() =
            getReferences().first()

    override fun getReferences() =
            when (flavor) {
                QualifiedValue ->           arrayOf(QualifiedValueReference(this, valueQID!!),
                                                    QualifiedValueModuleNameReference(this))
                QualifiedConstructor ->     arrayOf(QualifiedConstructorReference(this, upperCaseQID!!),
                                                    QualifiedConstructorModuleNameReference(this))
                BareValue ->                arrayOf(LexicalValueReference(this))
                BareConstructor ->          arrayOf(SimpleUnionOrRecordConstructorReference(this))
            }
}

/**
 * The module-prefix portion of the qualified value reference
 */
private class QualifiedValueModuleNameReference(valueExpr: ElmValueExpr): ElmQualifiedReferenceBase<ElmValueExpr>(valueExpr) {

    override val elementQID: ElmPsiElement
        get() = element.valueQID!!
}

/**
 * The module-prefix portion of the qualified union or record constructor reference
 */
private class QualifiedConstructorModuleNameReference(valueExpr: ElmValueExpr): ElmQualifiedReferenceBase<ElmValueExpr>(valueExpr) {

    override val elementQID: ElmPsiElement
        get() = element.upperCaseQID!!
}
