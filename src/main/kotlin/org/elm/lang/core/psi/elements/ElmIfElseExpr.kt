package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes

/**
 * An if-else expression, possible with one or more else-if branches.
 *
 * e.g.
 * - `if True then 1 else 2`
 * - `if False then 1 else if True then 2 else 3`
 */
class ElmIfElseExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag {

    /**
     * In a well-formed program, will contain an odd number of expressions, with at least three.
     */
    val expressionList: List<ElmExpressionTag>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExpressionTag::class.java)

    /** The `if` keywords. This will never be empty. */
    val ifKeywords : List<PsiElement> get() = findChildrenByType(ElmTypes.IF)
    /** The `then` keywords. In a well formed program, this will be the same size as [ifKeywords] */
    val thenKeywords : List<PsiElement> get() = findChildrenByType(ElmTypes.THEN)
    /** The `else` keywords. In a well formed program, this will be the same size as [ifKeywords] */
    val elseKeywords : List<PsiElement> get() = findChildrenByType(ElmTypes.ELSE)
}
