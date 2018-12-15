package org.elm.ide.typing

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.parentOfType
import org.elm.ide.inspections.MissingCaseBranchAdder
import org.elm.lang.core.psi.elements.ElmCaseOfBranch
import org.elm.lang.core.psi.elements.ElmCaseOfExpr

class ElmSmartEnterProcessor : SmartEnterProcessorWithFixers() {
    init {
        addFixers(CaseBranchFixer())
        addEnterProcessors(PlainEnterProcessor())
    }

    override fun getStatementAtCaret(editor: Editor?, psiFile: PsiFile?): PsiElement? {
        val statement = super.getStatementAtCaret(editor, psiFile)
        if (statement == null || statement is PsiWhiteSpace) return null
        if (statement.parentOfType<ElmCaseOfBranch>() != null) return null
        return statement.parentOfType<ElmCaseOfExpr>()
    }
}

private class PlainEnterProcessor : SmartEnterProcessorWithFixers.FixEnterProcessor() {
    override fun doEnter(atCaret: PsiElement, file: PsiFile, editor: Editor, modified: Boolean): Boolean {
        if (modified && atCaret is ElmCaseOfExpr && atCaret.branches.isNotEmpty()) {
            val caretModel = editor.caretModel
            val branch = atCaret.branches.first()
            val textOffset = branch.textOffset
            caretModel.moveToOffset(textOffset + branch.textLength)
        } else {
            plainEnter(editor)
        }
        return true
    }
}

private class CaseBranchFixer : SmartEnterProcessorWithFixers.Fixer<ElmSmartEnterProcessor>() {
    override fun apply(editor: Editor, processor: ElmSmartEnterProcessor, element: PsiElement) {
        if (element !is ElmCaseOfExpr || element.branches.isNotEmpty()) return
        MissingCaseBranchAdder(element).addMissingBranches()
    }
}
