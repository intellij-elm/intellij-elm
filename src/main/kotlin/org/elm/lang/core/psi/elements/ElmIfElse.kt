
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmIfElse(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * Will contain at least 2 expressions in a well-formed program.
     */
    val expressionList: List<ElmExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpression::class.java)

}
