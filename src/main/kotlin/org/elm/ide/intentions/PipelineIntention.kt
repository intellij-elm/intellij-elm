package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.withoutParens

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class PipelineIntention : ElmAtCaretIntentionActionBase<PipelineIntention.Context>() {

    sealed class Context {
        data class NoPipes(val functionCall: ElmFunctionCallExpr) : Context()
        data class HasRightPipes(val pipelineExpression: Pipeline.RightPipeline) : Context()
    }

    override fun getText() = "Use pipeline of |>"
    override fun getFamilyName() = text



    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {

        // find nearest ancestor (or self) that is
        // 1) a function call with at least one argument, or
        // 2) a pipeline that isn't fully piped (first part of the pipe has at least one argument)
        return element
                .ancestors
                .map {
            when (it) {
                is ElmBinOpExpr -> {
                    val pipeline = it.asPipeline()
                    pipeline?.let {
                        if (it is Pipeline.RightPipeline && it.isNonNormalizedRightPipeline()) {
                            Context.HasRightPipes(it)
                        } else {
                            null
                        }
                    }
                }
                is ElmFunctionCallExpr -> {
                    val parent = it.parent
                    if (parent is ElmBinOpExpr && isRightPipeline(parent)) {
                        null
                    } else {
                        if (it.arguments.count() > 0) {
                            Context.NoPipes(it)
                        } else {
                            null
                        }
                    }
                }
                else -> {
                    null
                }

            }
        }
                .filterNotNull()
                .firstOrNull()
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            val psiFactory = ElmPsiFactory(project)
            when (context) {
                is Context.NoPipes -> {
                    val existingIndent = DocumentUtil.getIndent(editor.document, context.functionCall.startOffset).toString()
                    val indent = context.functionCall.indentStyle.oneLevelOfIndentation
                    val rewrittenWithPipes = psiFactory.createPipeChain(existingIndent, indent, splitArgAndFunctionApplications(context.functionCall))
                    replaceUnwrapped(context.functionCall, rewrittenWithPipes)
                }
                is Context.HasRightPipes -> {
                    val existingIndent = DocumentUtil.getIndent(editor.document, context.pipelineExpression.pipeline.startOffset).toString()
                    val indent = context.pipelineExpression.pipeline.indentStyle.oneLevelOfIndentation
                    val segments = pipelineSegments(context.pipelineExpression.pipeline).drop(1)
                    val comments =
                            context.pipelineExpression
                                    .pipeline
                                    .partsWithComments
                                    .toList()
                                    .takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
                                    .filterIsInstance<PsiComment>()
                    val splitThing =
                            splitArgAndFunctionApplications(context.pipelineExpression.pipeline.parts.filterIsInstance<ElmFunctionCallExpr>().first())
                    val splitThingTransformed = splitThing.plus(comments)

                    val firstPartRewrittenWithPipeline = psiFactory.createPipeChain(
                            existingIndent,
                            indent,
                            splitThingTransformed
                                    .plus(segments)
                    )
                    replaceUnwrapped(context.pipelineExpression.pipeline, firstPartRewrittenWithPipeline)
                }
            }

        }
    }

    private fun pipelineSegments(originalPipeline: ElmBinOpExpr): List<Any> {
        var segments: List<String> = emptyList()
        var unprocessed = originalPipeline.partsWithComments
        while (true)  {
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


fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmParenthesizedExpr) {
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

private fun splitArgAndFunctionApplications(nestedFunctionCall: ElmFunctionCallExpr): List<String> {
    return when (nestedFunctionCall.arguments.count()) {
        0 -> {
            listOf(nestedFunctionCall.target.text)
        }
        else -> {
            val joinToString = sequenceOf(nestedFunctionCall.target).plus(nestedFunctionCall.arguments.take(nestedFunctionCall.arguments.count() - 1)).map { it.text }
                    .joinToString(separator = " ")

            processArgument(nestedFunctionCall.arguments.last()).plus(joinToString)
        }
    }
}

private fun processArgument(argument: ElmAtomTag): List<String> {
    val firstArgument = argument.withoutParens
    if (firstArgument is ElmFunctionCallExpr) {
        return splitArgAndFunctionApplications(firstArgument)
    }
    if (firstArgument.children.size != 1) {
        return listOf(addParensIfNeeded(firstArgument))
    }

    return if (firstArgument is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(firstArgument)
    } else {
        listOf(addParensIfNeeded(firstArgument))
    }
}

private fun addParensIfNeeded(element: ElmPsiElement): String {
    return if (needsParens(element)) {
        "(" + element.text + ")"
    } else {
        element.text
    }
}

private fun needsParens(element: ElmPsiElement): Boolean {
    return when (element) {
        is ElmFunctionCallExpr -> true
        is ElmBinOpExpr -> true
        is ElmAnonymousFunctionExpr -> true
        else -> false
    }
}

fun isRightPipeline(possiblePipeline: ElmBinOpExpr): Boolean {
    return (possiblePipeline.parts.any { it is ElmOperator && it.referenceName == "|>" })
}
