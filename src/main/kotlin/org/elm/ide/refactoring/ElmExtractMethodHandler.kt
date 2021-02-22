package org.elm.ide.refactoring

import com.intellij.openapi.editor.Editor
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.elements.ElmValueQID
import org.elm.openapiext.runWriteCommandAction

class ElmExtractMethodHandler : ElmExtractorBase() {

    override fun process(editor: Editor, chosenExpr: ElmExpressionTag) {
        if (!chosenExpr.isValid) return
        val project = editor.project ?: return

        val psiFactory = ElmPsiFactory(project)

        val fn: ElmValueDeclaration = getValueDeclaration(chosenExpr) ?: return

        val parameters = fn.declaredNames().map { it.name }.toList()

        val localDeclarations = fn.expression
            ?.descendantsOfType<ElmFunctionDeclarationLeft>()
            ?.map { it.lowerCaseIdentifier.text }
            .orEmpty()

        val declarations = (parameters + localDeclarations).toHashSet()

        val internalDeclarations = chosenExpr
            .descendantsOfType<ElmFunctionDeclarationLeft>()
            .map { it.lowerCaseIdentifier.text }
            .toHashSet()

        val usages: Set<String> = chosenExpr
            .descendantsOfType<ElmValueQID>()
            .mapNotNull { it.text }
            .filter { declarations.contains(it) && !internalDeclarations.contains(it) }
            .toHashSet()

        project.runWriteCommandAction {
            val valueDeclaration: ElmValueDeclaration = psiFactory.createValue(chosenExpr.text, usages)
            val fnEndOffset = fn.textRange.endOffset

            editor.document.insertString(fnEndOffset, "\n")
            editor.document.insertString(fnEndOffset, "\n")
            editor.document.insertString(fnEndOffset + 2, valueDeclaration.text)
            valueDeclaration.functionDeclarationLeft?.text?.let {
                editor.document.replaceString(chosenExpr.startOffset, chosenExpr.endOffset, it)
            }
        }
    }

    private fun getValueDeclaration(chosenExpr: ElmExpressionTag): ElmValueDeclaration? {
        var fn: ElmPsiElement? = chosenExpr

        while (fn != null) {
            when (fn) {
                is ElmValueDeclaration -> return fn
                else -> fn = fn.parent as ElmPsiElement?
            }
        }

        return null
    }
}
