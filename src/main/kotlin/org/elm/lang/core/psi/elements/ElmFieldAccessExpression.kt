package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER

/**
 * An expression which accesses a field on a record; can be chained
 *
 * e.g. `myRecord.foo.bar` and `{foo={bar=1}}.foo.bar`
 */
class ElmFieldAccessExpression(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {

    val start: ElmFieldAccessStart
        get() = findNotNullChildByClass(ElmFieldAccessStart::class.java)

    val firstField: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    val nextField: ElmFieldAccessContinue?
        get() = findChildByClass(ElmFieldAccessContinue::class.java)
}
