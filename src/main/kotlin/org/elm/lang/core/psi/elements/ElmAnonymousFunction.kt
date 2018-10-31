package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmNameDeclarationPatternTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * A lambda expression
 *
 * e.g. `\x -> x + 1`
 */
class ElmAnonymousFunction(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionCallTargetTag {

    /** Zero-or-more parameters to the lambda expression */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

    /** The body expression */
    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

    /** Named elements introduced by pattern destructuring in the parameter list */
    val namedParameters: List<ElmNameDeclarationPatternTag>
        get() = patternList.flatMap {
            PsiTreeUtil.collectElementsOfType(it, ElmNameDeclarationPatternTag::class.java)
        }

}
