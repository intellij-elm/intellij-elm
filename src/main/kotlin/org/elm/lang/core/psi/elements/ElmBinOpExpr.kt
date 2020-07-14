package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*


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

    sealed class Pipeline() {
        abstract val pipeline: ElmBinOpExpr

        data class LeftPipeline(override val pipeline: ElmBinOpExpr): Pipeline()
        data class RightPipeline(override val pipeline: ElmBinOpExpr): Pipeline()
    }

    fun asPipeline(): Pipeline? {
        parts.forEach {
            if (it is ElmOperator) {
                if (it.referenceName == "|>") {
                    return Pipeline.RightPipeline(this)
                } else if (it.referenceName == "<|") {
                    return Pipeline.LeftPipeline(this)
                }
            }
        }
        return null
    }
}
