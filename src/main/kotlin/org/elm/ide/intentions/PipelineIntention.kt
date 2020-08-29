package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.elements.ElmAnonymousFunctionExpr
import org.elm.lang.core.psi.elements.ElmBinOpExpr
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmParenthesizedExpr
import org.elm.lang.core.psi.elements.Pipeline.RightPipeline
import org.elm.lang.core.psi.indentStyle
import org.elm.lang.core.psi.oneLevelOfIndentation
import org.elm.lang.core.psi.prevSiblings
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.withoutExtraParens
import org.elm.lang.core.withoutParens

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class PipelineIntention : ElmAtCaretIntentionActionBase<PipelineIntention.Context>() {

    sealed class Context {
        data class NoPipes(val functionCall: ElmFunctionCallExpr) : Context()
        data class HasRightPipes(val pipelineExpression: RightPipeline) : Context()
    }

    override fun getText() = "Use pipeline of |>"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        // find nearest ancestor (or self) that is
        // 1) a function call with at least one argument, or
        // 2) a pipeline that isn't fully piped (first part of the pipe has at least one argument)
        element.ancestors.forEach {
            when (it) {
                is ElmFunctionCallExpr -> {
                    if (it.arguments.count() > 0 && !it.isPartOfAPipeline) {
                        return Context.NoPipes(it)
                    }
                }
                is ElmBinOpExpr -> {
                    it.asPipeline().let { pipe ->
                        if (pipe is RightPipeline && pipe.isNotFullyPiped) {
                            return Context.HasRightPipes(pipe)
                        }
                    }
                }
            }
        }
        return null
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
    return if (firstArgument is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(firstArgument, commentsForLastArg)
    } else if (firstArgument.children.size != 1) {
        listOf(addParensIfNeeded(firstArgument), commentsForLastArg)
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

private val ElmFunctionCallExpr.comments: Sequence<PsiComment>
    get() =
        directChildren.filterIsInstance<PsiComment>()

private val ElmFunctionCallExpr.isPartOfAPipeline: Boolean
    get() =
        (parent as? ElmBinOpExpr)?.asPipeline() is RightPipeline