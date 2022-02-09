package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.*
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

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? =
            element
                    .ancestors
                    .filter { isPipelineOperator(it) }
                    .firstOrNull()
                    ?.let { it.parent as? ElmBinOpExpr }
                    ?.asPipeline()?.let { Context(it) }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        project.runWriteCommandAction {
            val pipe = context.pipeline
            replaceUnwrapped(pipe.pipeline, normalizePipeline(pipe, project, editor))
        }
    }

    private fun normalizePipeline(originalPipeline: Pipeline, project: Project, editor: Editor): ElmPsiElement {
        val initial: ElmPsiElement? = null
        val existingIndent = DocumentUtil.getIndent(editor.document, originalPipeline.pipeline.startOffset).toString()
        val psiFactory = ElmPsiFactory(project)
        val isMultiline = isMultiline(originalPipeline)
        return originalPipeline.segments()
                .withIndex()
                .fold(initial, { functionCallSoFar, indexedSegment ->
                    val segment = indexedSegment.value
                    val indentation = existingIndent + "    ".repeat(indexedSegment.index)
                    val expressionString = segment.expressionParts.joinToString(" ") {
                        when {
                            isMultiline -> indentation + it.text
                            else -> it.text
                        }
                    }
                    val psi = when {
                        functionCallSoFar != null ->
                            psiFactory.callFunctionWithArgumentAndComments(segment.comments, expressionString, functionCallSoFar, indentation)

                        isMultiline ->
                            psiFactory.createParensWithComments(segment.comments, expressionString, indentation)

                        else ->
                            psiFactory.createParens(expressionString, indentation)
                    }
                    unwrapIfPossible(psi)
                })!!
    }

    private fun isMultiline(pipeline: Pipeline): Boolean =
            pipeline.segments().any { segment ->
                segment.comments.isNotEmpty() || segment.expressionParts.any { it.textContains('\n') }
            }

    private fun unwrapIfPossible(element: ElmParenthesizedExpr): ElmPsiElement {
        val wrapped = element.withoutExtraParens
        return when (val unwrapped = wrapped.withoutParens) {
            is ElmBinOpExpr,
            is ElmAnonymousFunctionExpr,
            is ElmFunctionCallExpr -> wrapped
            else -> unwrapped
        }
    }
}


private fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement) {
    val unwrapped = when (replaceWith) {
        is ElmParenthesizedExpr -> replaceWith.withoutParens
        else -> replaceWith
    }
    expression.replace(unwrapped)
}

fun isPipelineOperator(element: PsiElement): Boolean {
    return element is ElmOperator && (element.referenceName == "|>" || element.referenceName == "<|")
}
