
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


/**
 * A field assignment in a record literal.
 *
 * e.g. `name = "George"` in `{ name = "George", age = 42 }`
 */
class ElmField(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * The name of the field to bind to [expression].
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The field's expression value.
     */
    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

}
