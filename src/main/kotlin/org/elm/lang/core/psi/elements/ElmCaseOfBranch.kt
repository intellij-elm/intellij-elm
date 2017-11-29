package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl

class ElmCaseOfBranch(node: ASTNode) : ElmPsiElementImpl(node) {

    val pattern: ElmPattern
        get() = findNotNullChildByClass(ElmPattern::class.java)

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)


    /**
     * Named elements introduced by pattern destructuring on the left-hand side of the branch.
     */
    val destructuredNames: List<ElmNamedElement>
        get() = listOf(PsiTreeUtil.collectElementsOfType(this, ElmLowerPattern::class.java),
                       PsiTreeUtil.collectElementsOfType(this, ElmPatternAs::class.java)
                      ).flatten()
}
