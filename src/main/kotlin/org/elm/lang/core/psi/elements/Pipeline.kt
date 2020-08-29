package org.elm.lang.core.psi.elements

import com.intellij.psi.PsiComment
import org.elm.lang.core.psi.ElmBinOpPartTag

sealed class Pipeline {
    abstract val pipeline: ElmBinOpExpr
    abstract fun segments(): List<Segment>

    data class Segment(
            val expressionParts: List<ElmBinOpPartTag>,
            val comments: List<PsiComment>
    )

    data class LeftPipeline(override val pipeline: ElmBinOpExpr) : Pipeline() {
        override fun segments(): List<Segment> {
            val segments = mutableListOf<Segment>()
            var unprocessed = pipeline.parts.toList().reversed()
            while (true) {
                val currentPipeExpression = unprocessed
                        .takeWhile { !(it is ElmOperator && it.referenceName == "<|") }
                        .reversed()
                unprocessed = unprocessed.drop(currentPipeExpression.size + 1)
                segments += Segment(
                        currentPipeExpression.toList(),
                        currentPipeExpression.filterIsInstance<PsiComment>().toList()
                )

                if (currentPipeExpression.isEmpty() || unprocessed.isEmpty()) {
                    return segments
                }
            }
        }
    }

    data class RightPipeline(override val pipeline: ElmBinOpExpr) : Pipeline() {
        val isNotFullyPiped: Boolean
            get() =
                when (val firstPart = pipeline.parts.firstOrNull()) {
                    is ElmFunctionCallExpr -> firstPart.arguments.count() > 0
                    else -> false
                }


        override fun segments(): List<Segment> {
            var segments: List<Segment> = emptyList()
            var unprocessed = pipeline.partsWithComments
            var nextComments = emptyList<PsiComment>()
            while (true) {
                val takeWhile = unprocessed.takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
                unprocessed = unprocessed.drop(takeWhile.count() + 1)
                if (takeWhile.count() == 0 || unprocessed.count() == 0) {
                    nextComments += takeWhile.filterIsInstance<PsiComment>().toList()
                }
                val nextToAdd = Segment(
                        takeWhile.filterIsInstance<ElmBinOpPartTag>().toList(),
                        nextComments
                )
                nextComments = takeWhile.filterIsInstance<PsiComment>().toList()
                segments = segments.plus(nextToAdd)

                if (takeWhile.count() == 0 || unprocessed.count() == 0) {
                    return segments
                }
            }
        }
    }

}
