package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.utils.getIndent

/**
 * This intention is built to easily allow users to add log statements on values.
 * It is currently restricted from working fully with pipelines and function composition.
 * For pipelined operations, you can only log the complete statement (so logging the final output, in other words),
 * and function composition is ignored entirely
 * (since a composition produces a function, which can't be sensibly logged).
 *
 * Ideally, we would support inserting Debug.log statements inside a pipeline/composition,
 * to allow users to print the data getting piped along.
 */
class InlineDebugIntention : ElmAtCaretIntentionActionBase<InlineDebugIntention.Context>() {
    data class Context(val valueToDebug: ElmPsiElement)

    override fun getText() = "Log this value to console"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        element.ancestors.forEach {
            when (it) {
                is ElmValueExpr -> {
                    val functionCallParent = it.parentOfType<ElmFunctionCallExpr>()
                    val binOpParent = it.parentOfType<ElmBinOpExpr>()
                    return when {
                        functionCallParent != null && functionCallParent.target == it -> {
                            Context(functionCallParent)
                        }
                        binOpParent != null -> {
                            if (isFunComposition(binOpParent)) {
                                null
                            } else {
                                Context(binOpParent)
                            }
                        }
                        else -> {
                            Context(it)
                        }
                    }
                }
                is ElmCaseOfExpr -> {
                    return Context(it)
                }
                is ElmAtomTag -> {
                    return Context(it)
                }
                is ElmBinOpExpr -> {
                    return if (isFunComposition(it)) {
                        null
                    } else {
                        Context(it)
                    }
                }
            }
        }
        return null
    }

    private fun isFunComposition(opExpr: ElmBinOpExpr): Boolean =
            opExpr.parts
                    .filterIsInstance<ElmOperator>()
                    .map { it.referenceName }
                    .any { it == "<<" || it == ">>" }


    override fun invoke(project: Project, editor: Editor, context: Context) {
        val factory = ElmPsiFactory(project)
        val valueExpr = context.valueToDebug
        val itemsText = valueExpr.text
        val textLines = itemsText.lines()

        val debuggedText =
                if (textLines.size > 1) {
                    val baseIndent = editor.getIndent(valueExpr.startOffset)
                    val indentation = "    "
                    val indentedLines = textLines.mapIndexed { index, line -> baseIndent + indentation.repeat(index) + line }
                    """Debug.log
$baseIndent    "${textLines.first().escapeDoubleQuotes()} ..."
$baseIndent    (
$baseIndent    ${indentedLines.joinToString("\n")}
$baseIndent    )"""
                } else {
                    """Debug.log "${itemsText.escapeDoubleQuotes()}" ($itemsText)"""
                }

        valueExpr.replace(factory.createParens(debuggedText))
    }
}

private fun String.escapeDoubleQuotes(): String = replace("\"", "\\\"")
