package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that transforms a series of function applications to/from a pipeline.
 */
class PipelineIntention : ElmAtCaretIntentionActionBase<PipelineIntention.Context>() {

    sealed class Context {
        data class NoPipes(val functionCall: ElmFunctionCallExpr) : Context()
        data class HasRightPipes(val functionCall: ElmFunctionCallExpr, val target: ElmFunctionCallTargetTag, val arguments: Sequence<PsiElement>) : Context()
    }

    override fun getText() = "Pipeline"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return when (val functionCall = element.ancestors.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()) {
            is ElmFunctionCallExpr -> {
                if (functionCall.prevSiblings.withoutWsOrComments.toList().size >= 2) {

                    val (prev1, argument) = functionCall.prevSiblings.withoutWsOrComments.toList()
                    if (prev1 is ElmOperator && prev1.referenceName.equals("|>")) {
                        Context.HasRightPipes(functionCall, functionCall.target as ElmFunctionCallTargetTag, functionCall.arguments.plus(argument))
                    } else {
                        Context.NoPipes(functionCall)
                    }
                } else {
                    Context.NoPipes(functionCall)
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
                is Context.NoPipes -> {
                    if (context.functionCall.descendantsOfType<ElmFunctionCallExpr>().isEmpty()) {
                        val last = context.functionCall.arguments.last()
                        last.delete()
                        val thing = ElmPsiFactory(project).createPipe(last.text, context.functionCall.text)
                        context.functionCall.replace(thing)
                    } else {
                        val thing = ElmPsiFactory(project).createPipeChain(splitArgAndFunctionApplications(context.functionCall))
                        context.functionCall.replace(thing)
                    }
                }

                is Context.HasRightPipes -> {
                    val functionCallWithNoPipes = ElmPsiFactory(project)
                            .createParens(sequenceOf(context.functionCall.target).plus(context.arguments)
                            .map { it.text }.joinToString(separator = " "))
                    context.functionCall.parent.replace(functionCallWithNoPipes)
                }
            }
        }
    }
}

fun splitArgAndFunctionApplications (nestedFunctionCall : ElmFunctionCallExpr): List<String> {
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
        return listOf( firstArgument.text )
    }
    val thing2 = firstArgument.children.first()

    return if (thing2 is ElmFunctionCallExpr) {
        splitArgAndFunctionApplications(thing2)
    } else {
        listOf()
    }
}

fun unwrapParens(expression: ElmPsiElement): ElmPsiElement {
    return when (expression) {
        is ElmParenthesizedExpr -> {
            unwrapParens(expression.expression!!)
        }
        else -> {
            expression
        }
    }

}