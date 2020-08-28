package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.withoutExtraParens
import org.elm.lang.core.withoutParens

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class RemovePipelineIntention : ElmAtCaretIntentionActionBase<RemovePipelineIntention.Context>() {

    data class Context(val pipeline: Pipeline)

    override fun getText() = "Remove Pipes"
    override fun getFamilyName() = text


    private fun normalizePipeline(originalPipeline: Pipeline, project: Project, editor: Editor): ElmPsiElement {
        val initial: ElmPsiElement? = null
        val existingIndent = DocumentUtil.getIndent(editor.document, originalPipeline.pipeline.startOffset).toString()
        if (!isMultiline(originalPipeline)) {
            return originalPipeline.pipelineSegments()
                    .withIndex()
                    .fold(initial, { acc, indexedSegment ->
                        val segment = indexedSegment.value
                        val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                        if (acc == null) {
                            if (originalPipeline is Pipeline.RightPipeline) {
                                unwrapIfPossible(
                                        ElmPsiFactory(project).createParens(
                                                segment.expressionParts .map { it.text } .toList() .joinToString(separator = " ")
                                                , indentation
                                        )
                                )
                            } else {
                                val innerText =
                                                segment.expressionParts
                                                        .map { it.text }
                                                        .toList()
                                                        .joinToString(separator = " ")

                                unwrapIfPossible(ElmPsiFactory(project).createParens(innerText
                                        , indentation
                                )
                                )
                            }
                        } else {

                            val innerText = listOf(
                                    segment.expressionParts
                                            .map { it.text }
                                            .toList()
                                            .joinToString(separator = " ")

                            ).joinToString(separator = " ")
                            unwrapIfPossible(ElmPsiFactory(project).callFunctionWithArgumentAndComments(segment.comments, innerText , acc, indentation))
                        }
                    })!!
        }
        return originalPipeline.pipelineSegments()
                .withIndex()
                .fold(initial, { functionCallSoFar, indexedSegment ->
                    val segment = indexedSegment.value
                    val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                    if (functionCallSoFar == null) {
                        if (originalPipeline is Pipeline.RightPipeline) {
                            unwrapIfPossible(
                                    ElmPsiFactory(project).createParensWithComments(
                                            segment.comments,
                                                    segment.expressionParts
                                                    .map { indentation +  it.text }
                                                    .toList()
                                                    .joinToString(separator = " ")

                                            , indentation
                                    )
                            )
                        } else {
                            val innerText =
                                    segment.expressionParts
                                    .map { indentation + it.text }
                                    .toList()
                                    .joinToString(separator = " ")

                            unwrapIfPossible(ElmPsiFactory(project).createParensWithComments(segment.comments, innerText
                            , indentation
                            )
                            )
                        }
                    } else {

                        val innerText = segment.expressionParts
                                .map { indentation +  it.text }
                                        .toList()
                                .joinToString(separator = "\n")

                        unwrapIfPossible(ElmPsiFactory(project).callFunctionWithArgumentAndComments(segment.comments, innerText , functionCallSoFar, indentation))
                    }
                })!!
    }

    private fun isMultiline(pipeline: Pipeline): Boolean {
        val pipelineSegments = pipeline.pipelineSegments()
        val hasNewlines = pipelineSegments.any { it.expressionParts.any { part -> part.textContains('\n') } }
        val hasComments = pipelineSegments.any { it.comments.isNotEmpty() }
        return hasNewlines || hasComments
    }

    private fun unwrapIfPossible(element: ElmParenthesizedExpr): ElmPsiElement {
        val wrapped = element.withoutExtraParens
        return when (val unwrapped = wrapped.withoutParens) {
            is ElmBinOpExpr -> wrapped
            is ElmAnonymousFunctionExpr -> wrapped
            is ElmFunctionCallExpr -> wrapped
            else -> unwrapped
        }

    }

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return element
                .ancestors
                .filterIsInstance<ElmBinOpExpr>()
                .firstOrNull()
                ?.asPipeline()
                ?.let { Context(it) }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            when (val pipeline = context.pipeline) {
                is Pipeline.RightPipeline -> {
                    val replaceWith = normalizePipeline(pipeline, project, editor)
                    replaceUnwrapped(pipeline.pipeline, replaceWith)
                }
                is Pipeline.LeftPipeline -> {
                    replaceUnwrapped(pipeline.pipeline, normalizePipeline(pipeline, project, editor))
                }
            }
        }
    }
}


fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement) {
    return if (replaceWith is ElmParenthesizedExpr) {
        expression.replace(replaceWith.withoutParens)
        Unit
    } else {
        expression.replace(replaceWith)
        Unit
    }
}
