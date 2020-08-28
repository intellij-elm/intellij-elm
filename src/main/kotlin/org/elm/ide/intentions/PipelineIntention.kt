package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.withoutExtraParens
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
                .map { element ->
                    when (element) {
                is ElmBinOpExpr -> {
                    element.asPipeline()?.let { pipeline ->
                        if (pipeline is Pipeline.RightPipeline && pipeline.isNonNormalizedRightPipeline()) {
                            Context.HasRightPipes(pipeline)
                        } else {
                            null
                        }
                    }
                }
                is ElmFunctionCallExpr -> {
                    if ((element.parent as? ElmBinOpExpr)?.asPipeline() is Pipeline.RightPipeline) {
                        null
                    } else {
                        if (element.arguments.count() > 0) {
                            Context.NoPipes(element)
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
                    val indent = context.functionCall.indentStyle.oneLevelOfIndentation
                    val existingIndent = DocumentUtil.getIndent(editor.document, context.functionCall.startOffset).toString()
                    val rewrittenWithPipes = psiFactory.createPipeChain(existingIndent, indent, splitArgAndFunctionApplications(context.functionCall))
                    replaceUnwrapped(context.functionCall, rewrittenWithPipes, psiFactory)
                }
                is Context.HasRightPipes -> {
                    val existingIndent = DocumentUtil.getIndent(editor.document, context.pipelineExpression.pipeline.startOffset).toString()
                    val indent = context.pipelineExpression.pipeline.indentStyle.oneLevelOfIndentation
                    val firstSegment = context.pipelineExpression.pipelineSegments().first()
                    val segments = context.pipelineExpression.pipelineSegments().drop(1).flatMap {
                        it.comments.plus(it.expressionParts.map { it.text }.joinToString(separator = " "))
                    }
                    val splitApplications =
                            splitArgAndFunctionApplications(context.pipelineExpression.pipeline.parts.filterIsInstance<ElmFunctionCallExpr>().first())
                    val valueAndApplications = splitApplications.plus(firstSegment.comments)

                    replaceUnwrapped(context.pipelineExpression.pipeline,
                            psiFactory.createPipeChain(
                                    existingIndent,
                                    indent,
                                    valueAndApplications
                                            .plus(segments)
                            ), psiFactory)
                }
            }

        }
    }
}

fun replaceUnwrapped(expression: ElmPsiElement, replaceWith: ElmParenthesizedExpr, psiFactory: ElmPsiFactory) {
    val comments = replaceWith.directChildren.filterIsInstance<PsiComment>()
    val hasComments = comments.toList().isNotEmpty()
    if (needsParensInParent(expression) || hasComments) {
        expression.replace(replaceWith.withoutExtraParens)
    } else {
        val expression1 = replaceWith.withoutParens
        expression.replace(expression1)
    }
}

private fun needsParensInParent(element: ElmPsiElement): Boolean {
    return when (element.parent) {
        is ElmBinOpExpr -> true
        else -> false
    }
}

private fun splitArgAndFunctionApplications(nestedFunctionCall: ElmFunctionCallExpr, associatedComments: List<PsiComment> = emptyList()): List<Any> {
    return when (nestedFunctionCall.arguments.count()) {
        0 -> {
            val comments = nestedFunctionCall.comments.toList()
            listOf(associatedComments, comments, nestedFunctionCall.target.text)
        }
        else -> {
            val functionCallString = sequenceOf(nestedFunctionCall.target)
                    .plus(nestedFunctionCall.arguments.take(nestedFunctionCall.arguments.count() - 1))
                    .map { it.text }
                    .joinToString(separator = " ")

            val commentsForLastArg = nestedFunctionCall
                    .arguments
                    .last()
                    .prevSiblings
                    .toList()
                    .takeWhile { it is PsiWhiteSpace || it is PsiComment }.filterIsInstance<PsiComment>()

            val commentsForPreLastArg = nestedFunctionCall.comments.filter { !commentsForLastArg.contains(it) }

            processArgument(nestedFunctionCall.arguments.last(), commentsForLastArg).plus(associatedComments).plus(commentsForPreLastArg).plus(functionCallString)
        }
    }
}

private fun processArgument(argument: ElmAtomTag, commentsForLastArg: List<PsiComment>): List<Any> {
    val firstArgument = argument.withoutParens
    if (firstArgument is ElmFunctionCallExpr) {
        return splitArgAndFunctionApplications(firstArgument, commentsForLastArg)
    }
    if (firstArgument.children.size != 1) {
        return listOf(addParensIfNeeded(firstArgument), commentsForLastArg)
    }

    return if (firstArgument is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(firstArgument, commentsForLastArg)
    } else {
        commentsForLastArg.plus(listOf(addParensIfNeeded(firstArgument)))
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
