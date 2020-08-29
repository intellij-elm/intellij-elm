package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmAnonymousFunctionExpr
import org.elm.lang.core.psi.elements.ElmBinOpExpr
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmParenthesizedExpr
import org.elm.lang.core.psi.elements.Pipeline.RightPipeline
import org.elm.lang.core.withoutExtraParens
import org.elm.lang.core.withoutParens
import org.elm.openapiext.runWriteCommandAction

/**
 * An intention action that transforms a series of function applications into a pipeline.
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
        project.runWriteCommandAction {
            val psiFactory = ElmPsiFactory(project)
            when (context) {
                is Context.NoPipes -> {
                    val call = context.functionCall
                    val indent = call.indentStyle.oneLevelOfIndentation
                    val existingIndent = DocumentUtil.getIndent(editor.document, call.startOffset).toString()
                    val splitApplications = splitArgAndFunctionApplications(call)
                    val rewrittenWithPipes = psiFactory.createPipeChain(existingIndent, indent, splitApplications)
                    replaceUnwrapped(call, rewrittenWithPipes)
                }
                is Context.HasRightPipes -> {
                    val pipe = context.pipelineExpression
                    val existingIndent = DocumentUtil.getIndent(editor.document, pipe.pipeline.startOffset).toString()
                    val indent = pipe.pipeline.indentStyle.oneLevelOfIndentation
                    val firstSegment = pipe.segments().first()
                    val segments = pipe.segments().drop(1).flatMap {
                        it.comments.plus(it.expressionParts.joinToString(separator = " ") { it.text })
                    }
                    val splitApplications = splitArgAndFunctionApplications(
                            pipe.pipeline.parts.filterIsInstance<ElmFunctionCallExpr>().first()
                    )
                    val valueAndApplications = splitApplications + firstSegment.comments
                    val rewrittenFullyPiped = psiFactory.createPipeChain(existingIndent, indent, valueAndApplications + segments)
                    replaceUnwrapped(pipe.pipeline, rewrittenFullyPiped)
                }
            }
        }
    }
}

private fun replaceUnwrapped(expression: ElmPsiElement, parenExpr: ElmParenthesizedExpr) {
    val comments = parenExpr.directChildren.filterIsInstance<PsiComment>()
    val unwrapped = when {
        needsParensInParent(expression) || comments.count() > 0 ->
            parenExpr.withoutExtraParens
        else ->
            parenExpr.withoutParens
    }
    expression.replace(unwrapped)
}

private fun needsParensInParent(element: ElmPsiElement): Boolean =
        element.parent is ElmBinOpExpr

private fun splitArgAndFunctionApplications(call: ElmFunctionCallExpr, associatedComments: List<PsiComment> = emptyList()): List<Any> {
    val args = call.arguments
    return when (args.count()) {
        0 -> {
            listOf(associatedComments, call.comments.toList(), call.target.text)
        }
        else -> {
            val functionCallString = sequenceOf(call.target)
                    .plus(args.take(args.count() - 1))
                    .joinToString(separator = " ") { it.text }

            val commentsForLastArg = args
                    .last()
                    .prevSiblings
                    .toList()
                    .takeWhile { it is PsiWhiteSpace || it is PsiComment }
                    .filterIsInstance<PsiComment>()

            processArgument(args.last(), commentsForLastArg)
                    .plus(associatedComments)
                    .plus(call.comments.filter { it !in commentsForLastArg })
                    .plus(functionCallString)
        }
    }
}

private fun processArgument(argument: ElmAtomTag, commentsForLastArg: List<PsiComment>): List<Any> {
    val arg = argument.withoutParens
    return when {
        arg is ElmFunctionCallExpr -> {
            splitArgAndFunctionApplications(arg, commentsForLastArg)
        }
        arg.children.size != 1 -> {
            listOf(addParensIfNeeded(arg), commentsForLastArg)
        }
        else -> {
            commentsForLastArg + addParensIfNeeded(arg)
        }
    }
}

private fun addParensIfNeeded(element: ElmPsiElement): String =
        when {
            needsParens(element) -> "(" + element.text + ")"
            else -> element.text
        }

private fun needsParens(element: ElmPsiElement): Boolean =
        when (element) {
            is ElmFunctionCallExpr,
            is ElmBinOpExpr,
            is ElmAnonymousFunctionExpr -> true
            else -> false
        }

private val ElmFunctionCallExpr.comments: Sequence<PsiComment>
    get() =
        directChildren.filterIsInstance<PsiComment>()

private val ElmFunctionCallExpr.isPartOfAPipeline: Boolean
    get() =
        (parent as? ElmBinOpExpr)?.asPipeline() is RightPipeline