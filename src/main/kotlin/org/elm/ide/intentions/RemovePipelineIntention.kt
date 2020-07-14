package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.elmSettings
import org.elm.workspace.elmToolchain

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class RemovePipelineIntention : ElmAtCaretIntentionActionBase<RemovePipelineIntention.Context>() {

    sealed class Context {
        data class HasRightPipes(val functionCall: ElmBinOpExpr) : Context()
        data class HasLeftPipes(val functionCall: ElmBinOpExpr) : Context()
    }

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
                ?.let {
                    when (it) {
                        is ElmBinOpExpr.Pipeline.LeftPipeline -> {
                            Context.HasLeftPipes(it.thing)
                        }
                        is ElmBinOpExpr.Pipeline.RightPipeline -> {
                            Context.HasRightPipes(it.thing)
                        }
                    }
                }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            when (context) {
                is Context.HasRightPipes -> {
                    replaceUnwrapped(context.functionCall, findAndNormalize(context.functionCall, project))
                }
                is Context.HasLeftPipes -> {
                    val existingIndent = DocumentUtil.getIndent(editor.document, context.functionCall.startOffset).toString()
                    val indent = context.functionCall.indentStyle.oneLevelOfIndentation

                    val normalizedLeftPipeline = normalizeLeftPipeline(existingIndent, indent, context.functionCall.parts.toList(), project)
                    replaceUnwrapped(context.functionCall, normalizedLeftPipeline)
                }
            }
        }
    }
}


private fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmPsiElement) {
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
    return when (element.parent) {
        is ElmBinOpExpr -> true
        else -> false
    }
}
