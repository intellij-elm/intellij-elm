package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmNameDeclarationPatternTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes


/**
 * A pattern-matching branch in a case-of expression.
 */
class ElmCaseOfBranch(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * The pattern on the left-hand-side of the branch.
     */
    val pattern: ElmPattern
        get() = findNotNullChildByClass(ElmPattern::class.java)

    /**
     * The body expression on the right-hand-side of the branch.
     *
     * In a well-formed program, this will be non-null.
     */
    val expression: ElmExpressionTag?
        get() = findChildByClass(ElmExpressionTag::class.java)


    /**
     * Named elements introduced by pattern destructuring on the left-hand side of the branch.
     */
    val destructuredNames: List<ElmNameDeclarationPatternTag>
        get() = PsiTreeUtil.collectElementsOfType(pattern, ElmNameDeclarationPatternTag::class.java).toList()

    /** The `->` element. In a well formed program, this will not return null */
    val arrow: PsiElement? get() = findChildByType(ElmTypes.ARROW)
}
