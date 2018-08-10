package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmNameDeclarationPatternTag
import org.elm.lang.core.psi.tags.ElmOperandTag

/**
 * A lambda expression
 */
class ElmAnonymousFunction(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {

    /* Zero-or-more parameters to the lambda expression */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

    /* The body expression */
    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)


    /**
     * Named elements introduced by pattern destructuring in the parameter list
     */
    val namedParameters: List<ElmNameDeclarationPatternTag>
        get() = PsiTreeUtil.collectElementsOfType(this, ElmNameDeclarationPatternTag::class.java).toList()
}
