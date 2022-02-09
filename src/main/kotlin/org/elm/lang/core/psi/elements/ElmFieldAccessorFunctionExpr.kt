package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.RecordFieldReference
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.findTy

/**
 * A function expression that will access a field in a record.
 *
 * e.g. `.x` in `List.map .x [{x=1}]`
 */
class ElmFieldAccessorFunctionExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmFunctionCallTargetTag, ElmAtomTag {
    /** The name of the field being accessed */
    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    override val referenceNameElement: PsiElement
        get() = identifier

    override val referenceName: String
        get() = identifier.text

    override fun getReference(): ElmReference {
        return object : RecordFieldReference<ElmFieldAccessorFunctionExpr>(this@ElmFieldAccessorFunctionExpr) {
            override fun getTy(): Ty? = (element.findTy() as? TyFunction)?.parameters?.singleOrNull()
        }
    }
}
