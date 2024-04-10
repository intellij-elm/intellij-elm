package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmPatternChildTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmUnionPatternChildTag


/**
 * Pattern matching on the components of a tuple
 *
 * e.g. `(x, y)` in the function `scalePoint (x, y) s = (x * s, y * s)`
 */
class ElmTuplePattern(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionParamTag,
        ElmPatternChildTag, ElmUnionPatternChildTag {

    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)
}
