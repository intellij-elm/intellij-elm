package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * Pattern matching on the components of a tuple
 *
 * e.g. `(x, y)` in the function `scalePoint (x, y) s = (x * s, y * s)`
 */
class ElmTuplePattern(node: ASTNode) : ElmPsiElementImpl(node) {

    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)
}
