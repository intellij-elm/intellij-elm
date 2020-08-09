package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.resolve.scope.ExpressionScope
import org.elm.utils.getIndent

class MapToFoldIntention : ElmAtCaretIntentionActionBase<MapToFoldIntention.Context>() {
    data class Context(val mapInvocation: ElmFunctionCallExpr)

    override fun getFamilyName(): String {
        return ""
    }

    override fun getText(): String {
        return "Convert List.map to List.foldr"
    }

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        return element.ancestors.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()?.let {
            if (it.target.text == "List.map") {
                Context(it)
            } else {
                null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {

        val elmPsiFactory = ElmPsiFactory(project)
        val first = context.mapInvocation.arguments.toList().first()
        val second = context.mapInvocation.arguments.toList().getOrNull(1)

        val itemVariableName = uniqueName(context.mapInvocation, "item")
        val accVariableName = uniqueName(context.mapInvocation, "result")
        val mapFunctionExpression = first.text
        val itemsExpression = second?.let { it.text }.orEmpty()
        val multilineFunction = mapFunctionExpression.contains('\n')
        val existingIndent = editor.document!!.getIndent(context.mapInvocation.startOffset)

        val functionCallText =
                if (!multilineFunction) {
                    "List.foldr (\\$itemVariableName $accVariableName -> $mapFunctionExpression $itemVariableName :: $accVariableName) [] $itemsExpression"
                } else {
                    val indentedFunction = addIndentLevels("", 1, mapFunctionExpression)
                    val thing = """List.foldr
    (\$itemVariableName $accVariableName ->
"""
                    val thingAfter = """
                $itemVariableName
                :: $accVariableName
        )
        []
""".trimIndent()
                    val result =
                            addIndentLevels(existingIndent, 0, thing) +
                                    addIndentLevels("", 0, "    $existingIndent$indentedFunction") + "\n" +
                                    addIndentLevels(existingIndent, 1, thingAfter) + "\n" +
                                    "    $existingIndent$itemsExpression"

                    result
                }

        val createFunctionCallExpr = elmPsiFactory.createFunctionCallExpr(functionCallText)
        context.mapInvocation.replace(createFunctionCallExpr)
    }

    private fun uniqueName(element: ElmFunctionCallExpr, variableName: String, number: Int = 0): String {
        if (number == 0) {
            return if (ExpressionScope(element).getVisibleValues().mapNotNullTo(HashSet()) { it.name }.contains(variableName)) {
                return uniqueName(element, variableName, number + 1)
            } else {
                return variableName
            }
        }
        val concat = "$variableName$number"
        return if (ExpressionScope(element).getVisibleValues().mapNotNullTo(HashSet()) { it.name }.contains(concat)) {
            uniqueName(element, variableName, number + 1)
        } else {
            concat
        }
    }

}

fun addIndentLevels(existingIndent: String, indentBy: Int, original: String): String {
    return original.lines().map {
        if (it.isEmpty()) {
            it
        } else {
            existingIndent + " ".repeat(indentBy * 4) + it
        }
    }
            .joinToString(separator = "\n")
}
