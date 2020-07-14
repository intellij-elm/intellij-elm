package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class RemovePipelineIntention : ElmAtCaretIntentionActionBase<RemovePipelineIntention.Context>() {

    data class Context(val pipeline: ElmBinOpExpr.Pipeline)

    override fun getText() = "Remove Pipes"
    override fun getFamilyName() = text

    private fun normalizePipeline(originalPipeline: List<ElmPsiElement>, project: Project): ElmPsiElement {
        var soFar: ElmPsiElement? = null
        var unprocessed = originalPipeline
        while (true)  {
            val takeWhile = unprocessed.takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
            unprocessed = unprocessed.drop(takeWhile.size + 1)
            if (soFar == null) {
                soFar =
                        unwrapIfPossible(
                                ElmPsiFactory(project).createParens(
                                        takeWhile
                                                .map { it.text }
                                                .toList()
                                                .joinToString(separator = " ")

                                )
                        )
            } else {
                soFar = ElmPsiFactory(project).callFunctionWithArgument(
                        takeWhile
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
                        , soFar
                )

            }

            if (takeWhile.isEmpty() || unprocessed.isEmpty()) {
                return soFar
            }
        }
    }

    private fun unwrapIfPossible(element: ElmParenthesizedExpr): ElmPsiElement {
        return when (element.expression) {
            is ElmBinOpExpr -> element
            is ElmFunctionCallExpr -> element
            else -> element.expression!!
        }

    }

    private fun normalizeLeftPipeline(existingIndent: String, indent: String, originalPipeline: List<ElmPsiElement>, project: Project): ElmParenthesizedExpr {
        var soFar: ElmParenthesizedExpr? = null
        var unprocessed = originalPipeline.reversed()
        while (true)  {
            val currentPipeExpression = unprocessed
                    .takeWhile { !(it is ElmOperator && it.referenceName == "<|") }
                    .reversed()
            unprocessed = unprocessed.drop(currentPipeExpression.size + 1)
            if (soFar == null) {
                soFar = ElmPsiFactory(project).createParens(
                        currentPipeExpression

                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
                )
            } else {
                soFar = ElmPsiFactory(project).callFunctionWithArgumentWithIndent(
                        existingIndent,
                        indent,
                        currentPipeExpression
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
                        , soFar
                )

            }

            if (currentPipeExpression.isEmpty() || unprocessed.isEmpty()) {
                return soFar
            }

        }
    }

    private fun findAndNormalize(element: ElmBinOpExpr, project: Project): ElmPsiElement {
        return normalizePipeline(element.parts.toList(), project)
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
                is ElmBinOpExpr.Pipeline.RightPipeline -> {
                    replaceUnwrapped(pipeline.pipeline, findAndNormalize(pipeline.pipeline, project))
                }
                is ElmBinOpExpr.Pipeline.LeftPipeline -> {
                    val existingIndent = DocumentUtil.getIndent(editor.document, pipeline.pipeline.startOffset).toString()
                    val indent = pipeline.pipeline.indentStyle.oneLevelOfIndentation

                    val normalizedLeftPipeline = normalizeLeftPipeline(existingIndent, indent, pipeline.pipeline.parts.toList(), project)
                    replaceUnwrapped(pipeline.pipeline, normalizedLeftPipeline)
                }
            }
        }
    }
}


fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement) {
    return if (replaceWith is ElmParenthesizedExpr) {
        replaceUnwrappedHelper(expression, replaceWith)
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
