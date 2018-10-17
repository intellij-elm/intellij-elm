package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes

/**
 * A function expression that will access a field in a record.
 *
 * e.g. `.x` in `List.map .x [{x=1}]`
 */
class ElmFieldAccessorFunction(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionCallTargetTag, ElmOperandTag {
    /** The name of the field being accessed */
    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)
}
