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
            val initial =
                    listOf(Pair(listOf<PsiComment>(), listOf<ElmBinOpPartTag>()))

            val splitPipeline = pipeline.partsWithComments.toList().fold(initial, { acc, it ->
                if (it is ElmOperator && it.referenceName == "|>") {
                    // add new segment group to the stack
                    appendEmptyList(acc)
                } else {
                    mapLast(acc) { elementList ->
                        when (it) {
                            is PsiComment -> {
                                 Pair(elementList.first.plus(it), elementList.second)
                            }
                            is ElmBinOpPartTag -> {
                                Pair(elementList.first, elementList.second.plus(it))
                            } else -> {
                                TODO()
                            }
                        }
                    }
                }
            })

            val allComments = splitPipeline.map { it.first }
            val allExpressions = splitPipeline.map { it.second }

            return shiftComments(allComments).zip(allExpressions).map { pair ->
                        Segment(pair.second, pair.first)
            }
        }

    }

}

/* This gets us comments that will be empty at the top, and combines the last two lists of comments to ensure that the list size stays the same.
   We might be able to simplify this in the future by changing the logic for how we traverse a pipeline to extract its comments, and/or
   changing the logic for the order that we put comments from a Segment relative to the expressions. */
private fun shiftComments(allComments: List<List<PsiComment>>): MutableList<List<PsiComment>> {
    val allCommentsMapped = mutableListOf<List<PsiComment>>()
    allCommentsMapped.add(emptyList())
    allCommentsMapped.addAll(allComments)
    val lastTwo = allCommentsMapped.dropLast(2)
    allCommentsMapped.add(lastTwo.flatten())
    return allCommentsMapped
}

private fun mapLast(acc: List<Pair<List<PsiComment>, List<ElmBinOpPartTag>>>, list: (Pair<List<PsiComment>, List<ElmBinOpPartTag>>) -> Pair<List<PsiComment>, List<ElmBinOpPartTag>>): List<Pair<List<PsiComment>, List<ElmBinOpPartTag>>> {
    return acc.mapIndexed { index, elementList ->
        if (index == acc.count() - 1) {
            list(elementList)
        } else {
            elementList
        }
    }
}

private fun appendEmptyList(acc: List<Pair<List<PsiComment>, List<ElmBinOpPartTag>>>): List<Pair<List<PsiComment>, List<ElmBinOpPartTag>>> {
    listOf(Pair(listOf<PsiComment>(), listOf<ElmBinOpPartTag>()))
    val array = acc.toMutableList()
    array.add(Pair(emptyList(), emptyList()))
    return array.toList()
}
