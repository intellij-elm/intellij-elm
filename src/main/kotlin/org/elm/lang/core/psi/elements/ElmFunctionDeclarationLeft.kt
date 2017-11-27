
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElementImpl
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase


/**
 * A value or function declaration.
 *
 * Examples:
 *
 * A simple value:
 * `foo = 42`
 *
 * A function that takes 2 arguments:
 * `update msg model = model`
 *
 * @see [ElmOperatorDeclarationLeft]
 */
class ElmFunctionDeclarationLeft(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.LOWER) {

    /**
     * The name given to the function/value
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * Zero or more parameters to the function
     */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

}
