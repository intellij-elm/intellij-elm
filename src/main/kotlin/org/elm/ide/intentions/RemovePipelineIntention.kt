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
        val pipelineSegments = originalPipeline.pipelineSegments()
        val hasNewlines = pipelineSegments.any { it.expressionParts.any { it.textContains('\n') } }
        val hasComments = pipelineSegments.any { it.comments.isNotEmpty() }
        val multiline = hasNewlines || hasComments
        if (!multiline) {

            return pipelineSegments
                    .withIndex()
                    .fold(initial, { acc, indexedSegment ->
                        val segment = indexedSegment.value
                        val index = indexedSegment.index + 1
                        val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                        if (acc == null) {
                            if (originalPipeline is Pipeline.RightPipeline) {
                                // TODO this removes the comments but they need to be preserved
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
//                                    .map { it.text }
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
//                                .map { it.text }
                                            .map { it.text }
                                            .toList()
                                            .joinToString(separator = " ")

                            ).joinToString(separator = " ")

                            val thing = ElmPsiFactory(project).callFunctionWithArgument(innerText , acc, indentation)
                            unwrapIfPossible(thing)
                        }
                    })!!
        }
        return pipelineSegments
                .withIndex()
                .fold(initial, { acc, indexedSegment ->
                    val segment = indexedSegment.value
                    val index = indexedSegment.index + 1
                    val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                    val commentsText = "\n" + segment.comments
                            .map { indentation + it.text }
                            .toList()
                            .joinToString(separator = "\n")
                    if (acc == null) {
                        if (originalPipeline is Pipeline.RightPipeline) {
                            // TODO this removes the comments but they need to be preserved
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
//                                    .map { it.text }
                                    .toList()
                                    .joinToString(separator = " ")

                            unwrapIfPossible(ElmPsiFactory(project).createParensWithComments(segment.comments, innerText
                            , indentation
                            )
                            )
                        }
                    } else {

                        val innerText = segment.expressionParts
//                                .map { it.text }
                                .map { indentation +  it.text }
                                        .toList()
                                .joinToString(separator = "\n")

                        val thing = ElmPsiFactory(project).callFunctionWithArgumentAndComments(segment.comments, innerText , acc, indentation)
                        unwrapIfPossible(thing)
                    }
                })!!
    }

    private fun unwrapIfPossible(element: ElmParenthesizedExpr): ElmPsiElement {
        val wrapped = element.withoutExtraParens
        val unwrapped = wrapped.withoutParens
        return when (unwrapped) {
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
            val pipeline = context.pipeline
            when (pipeline) {
                is Pipeline.RightPipeline -> {
                    val replaceWith = normalizePipeline(pipeline, project, editor)
                    replaceUnwrapped(pipeline.pipeline, replaceWith, project)
                }
                is Pipeline.LeftPipeline -> {
                    replaceUnwrapped(pipeline.pipeline, normalizePipeline(pipeline, project, editor), project)
                }
            }
        }
    }
}


fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement, project: Project) {
    return if (replaceWith is ElmParenthesizedExpr) {
        expression.replace(replaceWith.withoutParens)
        Unit
    } else {
        expression.replace(replaceWith)
        Unit
    }
}
private fun replaceUnwrappedHelper(expression: ElmPsiElement, replaceWith: ElmParenthesizedExpr) {
    if (needsParensInParent(expression)) {
        expression.replace(replaceWith)
    } else {
        expression.replace(replaceWith.expression!!)
    }
}


private fun needsParensInParent(element: ElmPsiElement): Boolean {
    return element.parent is ElmBinOpExpr
}
