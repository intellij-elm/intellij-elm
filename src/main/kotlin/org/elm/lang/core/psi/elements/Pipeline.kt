package org.elm.lang.core.psi.elements

import com.intellij.psi.PsiComment
import org.elm.lang.core.psi.ElmBinOpPartTag

sealed class Pipeline {
    abstract val pipeline: ElmBinOpExpr
    abstract fun pipelineSegments(): List<PipelineSegment>

        data class LeftPipeline(override val pipeline: ElmBinOpExpr) : Pipeline() {
        override fun pipelineSegments(): List<PipelineSegment> {

            var segments: List<PipelineSegment> = emptyList()
            var unprocessed = pipeline.parts.toList().reversed()
            while (true)  {
                val currentPipeExpression = unprocessed
                        .takeWhile { !(it is ElmOperator && it.referenceName == "<|") }
                        .reversed()
                unprocessed = unprocessed.drop(currentPipeExpression.size + 1)
                val nextToAdd = PipelineSegment(
                        currentPipeExpression.filterIsInstance<ElmBinOpPartTag>().toList(),
                        currentPipeExpression.filterIsInstance<PsiComment>().toList()
                )
                segments = segments.plus(nextToAdd)

                if (currentPipeExpression.isEmpty() || unprocessed.isEmpty()) {
                    return segments
                }
            }
        }
    }

    data class RightPipeline(override val pipeline: ElmBinOpExpr) : Pipeline() {
        fun isNonNormalizedRightPipeline(): Boolean {
            return run {
                val firstPart =
                        pipeline
                                .parts
                                .firstOrNull()
                if (firstPart is ElmFunctionCallExpr) {
                    firstPart.arguments.count() > 0
                } else {
                    false
                }
            }
        }


        override fun pipelineSegments(): List<PipelineSegment> {
            var segments: List<PipelineSegment> = emptyList()
            var unprocessed = pipeline.partsWithComments
            while (true) {
                val takeWhile = unprocessed.takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
                unprocessed = unprocessed.drop(takeWhile.count() + 1)
                val nextToAdd = PipelineSegment(
                        takeWhile.filterIsInstance<ElmBinOpPartTag>().toList(),
                        takeWhile.filterIsInstance<PsiComment>().toList()
                )
                segments = segments.plus(nextToAdd)

                if (takeWhile.count() == 0 || unprocessed.count() == 0) {
                    return segments
                }
            }
        }
    }

}

data class PipelineSegment(val expressionParts: List<ElmBinOpPartTag>, val comments: List<PsiComment>)
