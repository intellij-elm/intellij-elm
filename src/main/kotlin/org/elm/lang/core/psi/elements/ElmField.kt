package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.RecordFieldReference


/**
 * A field assignment in a record literal.
 *
 * e.g. `name = "George"` in `{ name = "George", age = 42 }`
 */
class ElmField(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    /**
     * The name of the field to bind to [expression].
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The field's expression value.
     *
     * In a well-formed program, this will be non-null.
     */
    val expression: ElmExpressionTag?
        get() = findChildByClass(ElmExpressionTag::class.java)

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier

    override val referenceName: String
        get() = lowerCaseIdentifier.text

    override fun getReference(): ElmReference {
        return RecordFieldReference.fromElement(this) { it.parent as? ElmRecordExpr }
    }
}
