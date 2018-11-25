package org.elm.ide.typing

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiFile
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmStringConstantExpr

// A BackspaceHandlerDelegate is called during character deletion.
// We use this to delete triple quotes, since the QuoteHandler can only delete single characters.
class ElmBackspaceHandler : BackspaceHandlerDelegate() {
    private var rangeMarker: RangeMarker? = null

    // Called when a character is about to be deleted. There's no return value, so you can't affect behavior
    // here.
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        rangeMarker = null
        // If we didn't insert the matching quote, don't do anything
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) return
        if (file !is ElmFile) return

        val offset = editor.caretModel.offset
        val psiElement = file.findElementAt(offset) ?: return

        // We need to save the element range now, because the PSI will be changed by the time charDeleted is
        // called
        val parent = psiElement.parent ?: return
        if (parent is ElmStringConstantExpr
                && parent.text == "\"\"\"\"\"\""
                && editor.caretModel.offset == parent.textOffset + 3) {
            rangeMarker = editor.document.createRangeMarker(parent.textRange)
        }
    }

    // Called immediately after a character is deleted. If this returns true, no automatic quote or brace
    // deletion will happen.
    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        // The range marker is automatically adjusted with the deleted character, so we can just delete the
        // whole thing.
        rangeMarker?.let {
            editor.document.deleteString(it.startOffset, it.endOffset)
            rangeMarker = null
            return true
        }

        return false
    }

}
