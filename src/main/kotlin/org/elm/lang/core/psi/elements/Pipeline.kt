package org.elm.lang.core.psi.elements

sealed class Pipeline {
    abstract val pipeline: ElmBinOpExpr

    data class LeftPipeline(override val pipeline: ElmBinOpExpr) : Pipeline()

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

        fun pipelineSegments(): List<Any> {
            var segments: List<String> = emptyList()
            var unprocessed = pipeline.partsWithComments
            while (true) {
                val takeWhile = unprocessed.takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
                unprocessed = unprocessed.drop(takeWhile.count() + 1)
                val nextToAdd =
                        takeWhile
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
                segments = segments.plus(nextToAdd)

                if (takeWhile.count() == 0 || unprocessed.count() == 0) {
                    return segments
                }
            }
        }
    }

}
