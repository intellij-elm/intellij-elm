package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
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
class RemovePipelineIntention : ElmAtCaretIntentionActionBase<RemovePipelineIntention.Context>() {

    sealed class Context {
        data class HasRightPipes(val functionCall: ElmFunctionCallExpr, val target: ElmFunctionCallTargetTag, val arguments: Sequence<PsiElement>) : Context()
    }

    override fun getText() = "Remove Pipes"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return when (val functionCall = element.ancestors.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()) {
            is ElmFunctionCallExpr -> {
                if (functionCall.prevSiblings.withoutWsOrComments.toList().size >= 2) {

                    val (prev1, argument) = functionCall.prevSiblings.withoutWsOrComments.toList()
                    if (prev1 is ElmOperator && prev1.referenceName.equals("|>")) {
                        Context.HasRightPipes(functionCall, functionCall.target as ElmFunctionCallTargetTag, functionCall.arguments.plus(argument))
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            when (context) {
                is Context.HasRightPipes -> {
                    val rewrittenWithoutPipes = ElmPsiFactory(project).createParens(
                            sequenceOf(context.functionCall.target)
                                    .plus(context.arguments)
                                    .map { it.text }.joinToString(separator = " "))
                    context.functionCall.parent.replace(rewrittenWithoutPipes)
                }
            }

            if (project.elmSettings.toolchain.isElmFormatOnSaveEnabled) {
                tryElmFormat(project, editor)
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
    val singleArgument = firstArgument.children.first()

    return if (singleArgument is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(singleArgument)
    } else {
        listOf(singleArgument.text)
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