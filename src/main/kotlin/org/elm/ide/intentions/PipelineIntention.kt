package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that adds a function/type to a module's `exposing` list.
 */
class PipelineIntention : ElmAtCaretIntentionActionBase<PipelineIntention.Context>() {

    abstract class Context()

    data class NoPipes(val functionCall: ElmFunctionCallExpr) : Context()

    data class HasRightPipes(val functionCall: ElmFunctionCallExpr, val target: ElmFunctionCallTargetTag, val arguments : Sequence<PsiElement>) : Context()

    override fun getText() = "Pipeline"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return when (val functionCall = element.ancestors.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()) {
            is ElmFunctionCallExpr -> {
                if (functionCall.prevSiblings.withoutWsOrComments.toList().size >= 2) {

                    val (prev1, argument) = functionCall.prevSiblings.withoutWsOrComments.toList()
                    if (prev1 is ElmOperator && prev1.referenceName.equals("|>")) {
                        HasRightPipes(functionCall, functionCall.target as ElmFunctionCallTargetTag, functionCall.arguments.plus(argument))
                    } else {
                        NoPipes(functionCall)
                    }
                } else {
                    NoPipes(functionCall)
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
                is NoPipes -> {
                    if (context.functionCall.descendantsOfType<ElmFunctionCallExpr>().isEmpty()) {
                        val last = context.functionCall.arguments.last()
                        last.delete()
                        val thing = ElmPsiFactory(project).createPipe(last.text, context.functionCall.text)
                        context.functionCall.replace(thing)
                    } else {
                        val thing = ElmPsiFactory(project).createPipeChain(something(context.functionCall))
                        context.functionCall.replace(thing)
                    }
                }

                is HasRightPipes -> {
                    val functionCallWithNoPipes = ElmPsiFactory(project)
                            .createParens(sequenceOf(context.functionCall.target).plus(context.arguments)
                            .map { it.text }.joinToString(separator = " "))
                    context.functionCall.parent.replace(functionCallWithNoPipes)
                }

            }
        }
    }
}

fun something (nestedFunctionCall : ElmFunctionCallExpr): List<String> {
    if (nestedFunctionCall.arguments.toList().size != 1) {
        return listOf(nestedFunctionCall.text)
    }
    if (nestedFunctionCall.arguments.first().children.size != 1 ) {
        return listOf(
                nestedFunctionCall.arguments.first().text,
                nestedFunctionCall.target.text
        )
    }
    val thing2 = nestedFunctionCall.arguments.first().children.first()

    return if (thing2 is ElmFunctionCallExpr) {
        something(thing2).plus(nestedFunctionCall.target.text)
    } else {
        listOf(nestedFunctionCall.target.text)
    }

}