package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmAnonymousFunctionExpr
import org.elm.lang.core.psi.elements.ElmBinOpExpr
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmParenthesizedExpr
import org.elm.lang.core.psi.elements.Pipeline
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.withoutExtraParens
import org.elm.lang.core.withoutParens
import org.elm.openapiext.runWriteCommandAction

/**
 * An intention action that transforms a series of function applications from a pipeline.
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
            return originalPipeline.segments()
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
                            unwrapIfPossible(psiFactory.callFunctionWithArgumentAndComments(segment.comments, innerText, functionCallSoFar, indentation))
                        }
                    })!!
        }
        return originalPipeline.segments()
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
                                        expressionString, indentation)
                        )
                    } else {
                        unwrapIfPossible(psiFactory.callFunctionWithArgumentAndComments(segment.comments, expressionString, functionCallSoFar, indentation))
                    }
                })!!
    }

    private fun isMultiline(pipeline: Pipeline): Boolean {
        val pipelineSegments = pipeline.segments()
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

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? =
            element.ancestors
                    .filterIsInstance<ElmBinOpExpr>()
                    .firstOrNull()
                    ?.asPipeline()
                    ?.let { Context(it) }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        project.runWriteCommandAction {
            val pipe = context.pipeline
            replaceUnwrapped(pipe.pipeline, normalizePipeline(pipe, project, editor))
        }
    }
}


fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement) {
    val unwrapped = when (replaceWith) {
        is ElmParenthesizedExpr -> replaceWith.withoutParens
        else -> replaceWith
    }
    expression.replace(unwrapped)
}
