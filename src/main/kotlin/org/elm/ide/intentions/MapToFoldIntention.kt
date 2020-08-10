package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.ide.refactoring.uniqueValueName
import org.elm.lang.core.buildIndentedText
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.textWithNormalizedIndents

class MapToFoldIntention : ElmAtCaretIntentionActionBase<MapToFoldIntention.Context>() {
    data class Context(val mapInvocation: ElmFunctionCallExpr)

    override fun getText() = "Convert List.map to List.foldr"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? =
            element.ancestors.filterIsInstance<ElmFunctionCallExpr>()
                    .firstOrNull()
                    ?.let {
                        when (it.target.text) {
                            "List.map" -> Context(it)
                            else -> null
                        }
                    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val factory = ElmPsiFactory(project)
        val mapCallExpr = context.mapInvocation
        val itemVarName = uniqueValueName(mapCallExpr, "item")
        val accVarName = uniqueValueName(mapCallExpr, "result")
        val mapFuncText = mapCallExpr.arguments.toList().first().text
        val itemsText = mapCallExpr.arguments.toList().getOrNull(1)?.text.orEmpty()

        val newCallText = if (mapFuncText.contains("\n")) {
            // multi-line case
            buildIndentedText(mapCallExpr) {
                appendLine("List.foldr")
                level++
                appendLine("""(\$itemVarName $accVarName ->""")
                level++
                for (line in mapCallExpr.arguments.first().textWithNormalizedIndents.lines()) {
                    appendLine(line)
                }
                level++
                appendLine(itemVarName)
                appendLine(":: $accVarName")
                level -= 2
                appendLine(")")
                appendLine("[]")
                appendLine(itemsText)
            }

        } else {
            // single-line case
            "List.foldr (\\$itemVarName $accVarName -> $mapFuncText $itemVarName :: $accVarName) [] $itemsText"
        }

        mapCallExpr.replace(factory.createFunctionCallExpr(newCallText))
    }
}
