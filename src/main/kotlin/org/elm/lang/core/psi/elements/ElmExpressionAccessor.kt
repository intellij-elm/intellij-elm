package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER

/**
 * An accessor on a parenthesized expression or record.
 *
 * e.g. `.foo.bar` in `(fn arg).foo.bar` and `{foo={bar=1}}.foo.bar`
 */
class ElmExpressionAccessor(node: ASTNode) : ElmPsiElementImpl(node) {
    val lowerCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(LOWER_CASE_IDENTIFIER)
}
