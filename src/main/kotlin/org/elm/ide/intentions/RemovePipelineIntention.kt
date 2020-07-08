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
        data class HasRightPipes(val functionCall: ElmBinOpExpr) : Context()
        data class HasLeftPipes(val functionCall: ElmBinOpExpr) : Context()
    }

    override fun getText() = "Remove Pipes"
    override fun getFamilyName() = text

    private fun normalizePipeline(originalPipeline: List<ElmPsiElement>, project: Project): ElmPsiElement {
        var soFar: ElmParenthesizedExpr? = null
        var unprocessed = originalPipeline
        while (true)  {
            val takeWhile = unprocessed.takeWhile { !(it is ElmOperator && it.referenceName == "|>") }
            unprocessed = unprocessed.drop(takeWhile.size + 1)
            if (soFar == null) {
                soFar = ElmPsiFactory(project).createParens(
                        takeWhile
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ")
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

    private fun normalizeLeftPipeline(originalPipeline: List<ElmPsiElement>, project: Project): ElmPsiElement {
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
                soFar = ElmPsiFactory(project).callFunctionWithArgument(
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


        val possiblePipeline = element.ancestors.filterIsInstance<ElmBinOpExpr>().firstOrNull()

        return if (possiblePipeline == null) {
            null
        } else {
            val hasRightPipe = possiblePipeline.parts.any { it is ElmOperator && it.referenceName == "|>" }
            val hasLeftPipe = possiblePipeline.parts.any { it is ElmOperator && it.referenceName == "<|" }
            if (hasRightPipe) {
                Context.HasRightPipes(possiblePipeline)
            } else if (hasLeftPipe) {
                Context.HasLeftPipes(possiblePipeline)
            } else {
                null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            when (context) {
                is Context.HasRightPipes -> {
                    context.functionCall.replace(findAndNormalize(context.functionCall, project).originalElement!!)
                }
                is Context.HasLeftPipes -> {
                    val normalizedLeftPipeline = normalizeLeftPipeline(context.functionCall.parts.toList(), project)
                    context.functionCall.replace(normalizedLeftPipeline)
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

