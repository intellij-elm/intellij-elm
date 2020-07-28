package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr

class MapToFoldIntention : ElmAtCaretIntentionActionBase<MapToFoldIntention.Context>() {
    data class Context(val mapInvocation: ElmFunctionCallExpr)

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName(): String {
        return ""
    }


    override fun getText(): String {
        return "Convert List.map to List.foldr"
    }

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        // TODO return null if
        return Context(element.ancestors.filterIsInstance<ElmFunctionCallExpr>().first())
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {

        val elmPsiFactory = ElmPsiFactory(project)
        val first = context.mapInvocation.arguments.toList().first()
        val second = context.mapInvocation.arguments.toList().get(1)

        val functionName = "List.foldr"
        val innerFunctionName = first.text
        val itemsExpression = second.text
        val multilineFunction = innerFunctionName.contains('\n')
        val functionCallText =
                if (!multilineFunction) {
                    "$functionName (\\item result -> $innerFunctionName item :: result) [] $itemsExpression"
                } else {
                    """List.foldr
        (\item result ->
            greet
                (case thing of
                    Hello ->
                        "Hello"

                    Hi ->
                        "Hi"
                )
                item
                :: result
        )
        []
        names
""".trimIndent()
                }
        context.mapInvocation.replace(elmPsiFactory.createFunctionCallExpr(functionCallText))
    }

}
