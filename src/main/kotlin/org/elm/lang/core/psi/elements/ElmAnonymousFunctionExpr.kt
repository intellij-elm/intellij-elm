package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*

/**
 * A lambda expression
 *
 * e.g. `\x -> x + 1`
 */
class ElmAnonymousFunctionExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFunctionCallTargetTag {

    /** Zero-or-more parameters to the lambda expression */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

    /** The body expression */
    val expression: ElmExpressionTag
        get() = findNotNullChildByClass(ElmExpressionTag::class.java)

    /** Named elements introduced by pattern destructuring in the parameter list */
    val namedParameters: List<ElmNameDeclarationPatternTag>
        get() = patternList.flatMap {
            PsiTreeUtil.collectElementsOfType(it, ElmNameDeclarationPatternTag::class.java)
        }

}
