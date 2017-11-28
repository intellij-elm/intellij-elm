
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference


/**
 * A type annotation
 *
 * e.g. `length : String -> Int`
 *
 * Either [lowerCaseIdentifier] or [operatorAsFunction] is non-null
 */
class ElmTypeAnnotation(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    /**
     * The left-hand side of the type annotation which names the value
     *
     * e.g. `length` in `length : String -> Int`
     */
    val lowerCaseIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The left-hand side when the value is a binary operator instead of a normal identifier.
     *
     * e.g. `(++)` in `(++) : String -> String -> String`
     */
    val operatorAsFunction: ElmOperatorAsFunction?
        get() = findChildByClass(ElmOperatorAsFunction::class.java)

    /**
     * The right-hand side of the type annotation which describes the type of the value.
     *
     * e.g. `String -> Int` in `length : String -> Int`
     */
    val typeRef: ElmTypeRef
        get() = findNotNullChildByClass(ElmTypeRef::class.java)

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier
                    ?: operatorAsFunction?.operator
                    ?: throw RuntimeException("cannot determine type annotations's ref name element")

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            LocalTopLevelValueReference(this)
}
