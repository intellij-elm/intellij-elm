package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.withoutExtraParens
import org.elm.lang.core.withoutParens

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class RemovePipelineIntention : ElmAtCaretIntentionActionBase<RemovePipelineIntention.Context>() {

    data class Context(val pipeline: Pipeline)

    override fun getText() = "Remove Pipes"
    override fun getFamilyName() = text


    private fun normalizePipeline(originalPipeline: Pipeline, project: Project): ElmPsiElement {
        var initial: ElmPsiElement? = null
        return originalPipeline
                .pipelineSegments()
                .fold(initial, { acc, segment ->
                    if (acc == null) {
                        if (originalPipeline is Pipeline.RightPipeline) {
                            unwrapIfPossible(
                                    ElmPsiFactory(project).createParens(

                                            segment.expressionParts
                                                    .map { it.text }
                                                    .toList()
                                                    .joinToString(separator = " ") +
                                                    "\n\n" +
                                                    segment.comments
                                                            .map { it.text }
                                                            .toList()
                                                            .joinToString(separator = "\n")


                                    )
                            )
                        } else {
                            val innerText = segment.expressionParts
                                    .map { it.text }
                                    .toList()
                                    .joinToString(separator = " ") + "\n" +
                                    segment.comments
                                            .map { it.text }
                                            .toList()
                                            .joinToString(separator = "\n")
                        ElmPsiFactory(project).createParens(innerText)
                        }
                    } else {

                        val innerText = segment.expressionParts
                                .map { it.text }
                                .toList()
                                .joinToString(separator = " ") + "\n" +
                                segment.comments
                                        .map { it.text }
                                        .toList()
                                        .joinToString(separator = "\n")
                        ElmPsiFactory(project).callFunctionWithArgument(innerText , acc)
                    }
                })!!
    }

    private fun unwrapIfPossible(element: ElmParenthesizedExpr): ElmPsiElement {
        val wrapped = element.withoutExtraParens
        val unwrapped = wrapped.withoutParens
        return when (unwrapped) {
            is ElmBinOpExpr -> wrapped
            is ElmAnonymousFunctionExpr -> wrapped
            is ElmFunctionCallExpr -> wrapped
            else -> unwrapped
        }

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
                is Pipeline.RightPipeline -> {
                    replaceUnwrapped(pipeline.pipeline, normalizePipeline(pipeline, project))
                }
                is Pipeline.LeftPipeline -> {
                    replaceUnwrapped(pipeline.pipeline, normalizePipeline(pipeline, project))
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
