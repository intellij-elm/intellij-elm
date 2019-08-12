package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.Flavor.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.*


enum class Flavor {
    QualifiedValue,
    QualifiedConstructor,
    BareValue,
    BareConstructor
}

/**
 * An expression that is a reference to a parameter, bound pattern, function or constructor, optionally
 * qualified with a module name.
 *
 * e.g.
 *  - `Route.fromUrl`
 *  - `Json.Decode.Value`
 *  - `x`
 */
class ElmValueExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmAtomTag, ElmFunctionCallTargetTag, ElmFieldAccessTargetTag {

    val valueQID: ElmValueQID?
        get() = findChildByClass(ElmValueQID::class.java)

    val upperCaseQID: ElmUpperCaseQID?
        get() = findChildByClass(ElmUpperCaseQID::class.java)

    val qid: ElmQID
        get() = valueQID ?: upperCaseQID!!

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
            QualifiedValue -> valueQID!!.lowerCaseIdentifier
            QualifiedConstructor -> upperCaseQID!!.upperCaseIdentifierList.last()
            BareValue -> valueQID!!.lowerCaseIdentifier
            BareConstructor -> upperCaseQID!!.upperCaseIdentifierList.last()
        }

    /**
     * Return the name of the principal reference (the type or value, not the module prefix)
     */
    override val referenceName: String
        get() = referenceNameElement.text


    override fun getReference(): ElmReference =
            getReferences().first()

    override fun getReferences(): Array<ElmReference> =
            when (flavor) {
                QualifiedValue -> arrayOf(
                        QualifiedValueReference(this, valueQID!!),
                        ModuleNameQualifierReference(this, valueQID!!, valueQID!!.qualifierPrefix)
                )
                QualifiedConstructor -> arrayOf(
                        QualifiedConstructorReference(this, upperCaseQID!!),
                        ModuleNameQualifierReference(this, upperCaseQID!!, upperCaseQID!!.qualifierPrefix)
                )
                BareValue -> arrayOf(LexicalValueReference(this))
                BareConstructor -> arrayOf(SimpleUnionOrRecordConstructorReference(this))
            }
}
