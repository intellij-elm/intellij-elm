package org.elm.ide.refactoring

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.elm.ide.utils.findExpressionAtCaret
import org.elm.ide.utils.findExpressionInRange
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmLetInExpr
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.openapiext.runWriteCommandAction

class ElmExtractMethodHandler : RefactoringActionHandler {

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is ElmFile) return
        val exprs = findCandidateExpressions(editor, file)
        when (exprs.size) {
            0 -> {
                val message = RefactoringBundle.message(
                    if (editor.selectionModel.hasSelection())
                        "selected.block.should.represent.an.expression"
                    else
                        "refactoring.introduce.selection.error"
                )
                val title = RefactoringBundle.message("introduce.variable.title")
                val helpId = "refactoring.extractVariable"
                CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId)
            }
            1 -> introduceVariable(editor, exprs.single())
            else -> showExpressionChooser(editor, exprs) { chosenExpr ->
                introduceVariable(editor, chosenExpr)
            }
        }
    }

    private fun findCandidateExpressions(editor: Editor, file: ElmFile): List<ElmExpressionTag> {
        val selection = editor.selectionModel
        return if (selection.hasSelection()) {
            // If the user has some text selected, make a single suggestion based on the selection
            listOfNotNull(findExpressionInRange(file, selection.selectionStart, selection.selectionEnd))
        } else {
            // Suggest nested expressions at caret position
            val expr = findExpressionAtCaret(file, editor.caretModel.offset) ?: return emptyList()
            expr.ancestors
                .takeWhile { it !is ElmValueDeclaration && it !is ElmLetInExpr }
                .filterIsInstance<ElmExpressionTag>()
                .toList()
        }
    }

    private fun introduceVariable(editor: Editor, chosenExpr: ElmExpressionTag) {
        if (!chosenExpr.isValid) return
        val project = editor.project ?: return

        val psiFactory = ElmPsiFactory(project)

        val fn: ElmValueDeclaration = getValueDeclaration(chosenExpr) ?: return

        val declarations: List<String> = fn.declaredNames().map { it.name }.toList() +
            fn.expression?.descendantsOfType<ElmFunctionDeclarationLeft>()?.map { it.lowerCaseIdentifier.text }.orEmpty()

        val declarationsMap = declarations.toHashSet()

        val declarationUsages: List<String> = chosenExpr.text.split(" ").filter { declarationsMap.contains(it) }

        project.runWriteCommandAction {
            val valueDeclaration: ElmValueDeclaration = psiFactory.createValue(chosenExpr.text, declarationUsages)
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

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // IntelliJ will never call this when introducing a variable
    }
}
