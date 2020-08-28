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
        val psiFactory = ElmPsiFactory(project)
        if (!isMultiline(originalPipeline)) {
            return originalPipeline.pipelineSegments()
                    .withIndex()
                    .fold(initial, { functionCallSoFar, indexedSegment ->
                        val segment = indexedSegment.value
                        val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                        val expressionString = segment.expressionParts
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
                        if (functionCallSoFar == null) {
                            unwrapIfPossible(psiFactory.createParens(expressionString, indentation))
                        } else {
                            val innerText = listOf(expressionString).joinToString(separator = " ")
                            unwrapIfPossible(psiFactory.callFunctionWithArgumentAndComments(segment.comments, innerText , functionCallSoFar, indentation))
                        }
                    })!!
        }
        return originalPipeline.pipelineSegments()
                .withIndex()
                .fold(initial, { functionCallSoFar, indexedSegment ->
                    val segment = indexedSegment.value
                    val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                    val expressionString = segment.expressionParts
                            .map { indentation + it.text }
                            .toList().joinToString(separator = " ")
                    if (functionCallSoFar == null) {
                        unwrapIfPossible(
                                psiFactory.createParensWithComments(segment.comments,
                                        expressionString
                                        , indentation )
                        )
                    } else {
                        unwrapIfPossible(psiFactory.callFunctionWithArgumentAndComments(segment.comments, expressionString , functionCallSoFar, indentation))
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
