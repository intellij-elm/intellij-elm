package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmFieldAccessTargetTag
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.RecordFieldReference

/**
 * Accessing a field on a record.
 *
 * EXAMPLES: The following expressions each access a record field called `name`:
 * ```
 * user.name
 * model.currentUser.name
 * (defaultUser "George").name
 * { user = { name = "George" } }.name
 * ```
 */
class ElmFieldAccessExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmAtomTag, ElmFunctionCallTargetTag, ElmFieldAccessTargetTag {

    /** An expression which evaluates to a record value whose field we want to access */
    val targetExpr: ElmFieldAccessTargetTag
        get() = findNotNullChildByClass(ElmFieldAccessTargetTag::class.java)

    /** The name of the record field to read */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier

    override val referenceName: String
        get() = lowerCaseIdentifier.text

    override fun getReference(): ElmReference {
        return RecordFieldReference.fromElement(this) { it.targetExpr }
    }
}
