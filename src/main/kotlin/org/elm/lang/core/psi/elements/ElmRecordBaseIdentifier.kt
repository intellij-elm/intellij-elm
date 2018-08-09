package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.LexicalValueReference
import org.elm.lang.core.resolve.reference.TypeVariableReference

/**
 * This can occur in 2 different contexts:
 *
 * - In a record literal update expression, the name of an existing record which is to be updated.
 *     - e.g. the first instance of person in `{ person | age = person.age + 1 }`
 *     - in this case, the identifier acts as reference to a name in the lexical scope
 *
 * - In an extension record type expression, the base record type variable
 *     - e.g. the `a` immediately before the pipe character in `type alias User a = { a | name : String }
 *     - e.g. the `a` in `foo : { a | name : String } -> String`
 *     - in this case, the identifier acts as a type variable which may or may not resolve
 *       (the type alias example above will resolve, but the function parameter example will not resolve)
 *
 */
class ElmRecordBaseIdentifier(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {
    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference = references.first()

    override fun getReferences(): Array<ElmReference> {
        return when (parent) {
            is ElmRecordType -> arrayOf(TypeVariableReference(this))
            is ElmRecord -> arrayOf(LexicalValueReference(this))
            else -> emptyArray()
        }
    }
}
