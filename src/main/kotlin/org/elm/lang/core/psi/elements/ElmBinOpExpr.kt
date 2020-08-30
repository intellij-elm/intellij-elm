package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.Pipeline.LeftPipeline
import org.elm.lang.core.psi.elements.Pipeline.RightPipeline


/**
 * One or more binary operator expressions.
 *
 * Examples:
 *
 *  ```
 *  4 + 3
 *  4 + 3 == 7
 *  String.fromInt x ++ " things"
 *  ```
 */
class ElmBinOpExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmExpressionTag {

    /**
     * An interleaved sequence of operands and binary operators.
     *
     * For example, `String.fromInt x ++ " things"` would be a 3-element sequence consisting of
     * [ElmFunctionCallExpr] followed by [ElmOperator] followed by [ElmAtomTag].
     */
    val parts: Sequence<ElmBinOpPartTag> get() = directChildren.filterIsInstance<ElmBinOpPartTag>()
    val partsWithComments: Sequence<PsiElement> get() = directChildren.filter { it is PsiComment || it is ElmBinOpPartTag }


    fun asPipeline(): Pipeline? {
        parts.forEach {
            if (it is ElmOperator) {
                when (it.referenceName) {
                    "<|" -> return LeftPipeline(this)
                    "|>" -> return RightPipeline(this)
                }
            }
        }
        return null
    }
}
