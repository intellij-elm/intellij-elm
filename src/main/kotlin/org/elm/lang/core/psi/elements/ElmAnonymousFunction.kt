package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * A lambda expression
 */
class ElmAnonymousFunction(node: ASTNode) : ElmPsiElementImpl(node) {

    /* Zero-or-more parameters to the lambda expression */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

    /* The body expression */
    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)



    /**
     * Named elements introduced by pattern destructuring in the parameter list
     */
    val namedParameters: List<ElmNamedElement>
        get() {
            val results = mutableListOf<ElmNamedElement>()
            results.addAll(PsiTreeUtil.collectElementsOfType(this, ElmLowerPattern::class.java))
            results.addAll(PsiTreeUtil.collectElementsOfType(this, ElmPatternAs::class.java))
            return results
        }
}
