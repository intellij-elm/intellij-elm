package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.elmSettings
import org.elm.workspace.elmToolchain

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class PipelineIntention : ElmAtCaretIntentionActionBase<PipelineIntention.Context>() {

    sealed class Context {
        data class NoPipes(val functionCall: ElmFunctionCallExpr) : Context()
        data class HasRightPipes(val pipelineExpression: ElmBinOpExpr) : Context()
    }

    override fun getText() = "Use pipeline of |>"
    override fun getFamilyName() = text

    private fun isNonNormalizedRightPipeline(possiblePipeline: ElmBinOpExpr): Boolean {
        return if (possiblePipeline.parts.any { it is ElmOperator && it.referenceName == "|>" }) {
            val firstPart = possiblePipeline
                    .parts
                    .firstOrNull()
            if (firstPart is ElmFunctionCallExpr) {
                firstPart.arguments.count() > 0
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {

        // find nearest ancestor (or self) that is
        // 1) a function call with at least one argument, or
        // 2) a pipeline that isn't fully piped (first part of the pipe has at least one argument)
        return element.ancestors.map {
            when (it) {
                is ElmBinOpExpr -> {
                    if (isNonNormalizedRightPipeline(it)) {
                        Context.HasRightPipes(it)
                    } else {
                        null
                    }
                }
                is ElmFunctionCallExpr -> {
                    val parent = it.parent
                    if (parent is ElmBinOpExpr && isNonNormalizedRightPipeline(parent)) {
                        Context.HasRightPipes(parent)
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
            when (context) {
                is Context.NoPipes -> {
                    if (context.functionCall.descendantsOfType<ElmFunctionCallExpr>().isEmpty()) {
                        val lastArgument = context.functionCall.arguments.last()
                        lastArgument.delete()
                        val rewrittenWithPipes = ElmPsiFactory(project).createPipe(lastArgument.text, context.functionCall.text)
                        context.functionCall.replace(rewrittenWithPipes)
                    } else {
                        val rewrittenWithPipes = ElmPsiFactory(project).createPipeChain(splitArgAndFunctionApplications(context.functionCall))
                        context.functionCall.replace(rewrittenWithPipes)
                    }
                }
                is Context.HasRightPipes -> {
                    val segments = pipelineSegments(context.pipelineExpression).drop(1)
                    val comments =
                            context.pipelineExpression
                                    .partsWithComments
                                    .toList()
                                    .takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
                                    .filterIsInstance<PsiComment>()
                    val splitThing =
                            splitArgAndFunctionApplications(context.pipelineExpression.parts.filterIsInstance<ElmFunctionCallExpr>().first())
                    val splitThingTransformed = splitThing.plus(comments)

                    val firstPartRewrittenWithPipeline = ElmPsiFactory(project).createPipeChain(
                            splitThingTransformed
                                    .plus(segments)
                    )


                    context.pipelineExpression.replace(firstPartRewrittenWithPipeline)
                }
            }

            if (project.elmSettings.toolchain.isElmFormatOnSaveEnabled) {
                tryElmFormat(project, editor)
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

    private fun tryElmFormat(project: Project, editor: Editor) {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

        try {
            val vFile = FileDocumentManager.getInstance().getFile(editor.document)
            val elmVersion = ElmFormatCLI.getElmVersion(project, vFile!!)
            val elmFormat = project.elmToolchain.elmFormatCLI
            elmFormat!!.formatDocumentAndSetText(project, editor.document, elmVersion!!, addToUndoStack = false)
        } catch (e: Throwable) {
        }
    }
}

private fun splitArgAndFunctionApplications(nestedFunctionCall: ElmFunctionCallExpr): List<String> {
    if (nestedFunctionCall.arguments.count() == 0) {
        return listOf(nestedFunctionCall.text)
    }
    return when (nestedFunctionCall.arguments.count()) {
        0 -> {
            listOf(nestedFunctionCall.target.text)
        }
        1 -> {
            processArgument(nestedFunctionCall.arguments.last()).plus(nestedFunctionCall.target.text)
        }
        else -> {
            val joinToString = sequenceOf(nestedFunctionCall.target).plus(nestedFunctionCall.arguments.take(nestedFunctionCall.arguments.count() - 1)).map { it.text }
                    .joinToString(separator = " ")

            processArgument(nestedFunctionCall.arguments.last()).plus(joinToString)
        }

    }

}

private fun processArgument(argument: ElmAtomTag): List<String> {
    val firstArgument = unwrapParens(argument)
    if (firstArgument is ElmFunctionCallExpr) {
        return splitArgAndFunctionApplications(firstArgument)
    }
    if (firstArgument.children.size != 1) {
        return listOf(firstArgument.text)
    }

    return if (firstArgument is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(firstArgument)
    } else {
        listOf(firstArgument.text)
    }
}

private fun unwrapParens(expression: ElmPsiElement): ElmPsiElement {
    return when (expression) {
        is ElmParenthesizedExpr -> {
            unwrapParens(expression.expression!!)
        }
        else -> {
            expression
        }
    }

}
