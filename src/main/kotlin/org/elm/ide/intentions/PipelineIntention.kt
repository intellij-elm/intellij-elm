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

    data class Context(val functionCall: ElmFunctionCallExpr)

    override fun getText() = "Pipeline"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return when (val functionCall = element.ancestors.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()) {
            is ElmFunctionCallExpr -> {
                Context(functionCall)
            }
            else -> {
                null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            val (arg1, arg2) = context.functionCall.arguments.toList()
            arg2.delete()
            val thing = ElmPsiFactory(project).createPipe(arg2.text, context.functionCall.text)
            context.functionCall.replace(thing)
        }
    }
}
