package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.DOT
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER

/**
 * Access a field on a record
 *
 * e.g. `.foo` in `(fn arg).foo` and `{foo={bar=1}}.foo`
 */
class ElmFieldAccessContinue(node: ASTNode) : ElmPsiElementImpl(node) {

    val dot: PsiElement
        get() = findNotNullChildByType(DOT)

    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    val nextField: ElmFieldAccessContinue?
        get() = findChildByClass(ElmFieldAccessContinue::class.java)
}
