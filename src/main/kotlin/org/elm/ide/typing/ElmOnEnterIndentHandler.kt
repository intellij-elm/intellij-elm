/*
* Use of this source code is governed by the MIT license that can be
* found in the LICENSE file.
*
* Based on RsEnterInLineCommentHandler from intellij-rust
*/

package org.elm.ide.typing

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.text.CharArrayUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmCaseOfBranch
import org.elm.lang.core.psi.elements.ElmRecordExpr
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * Poor-man's formatter for Elm code. In the long-term it would be nice
 * to properly implement Elm source code formatting within IntelliJ, but
 * the fact is that most people will be using the external `elm-format`
 * tool.
 *
 * So the idea behind this handler is just to do the minimum amount of
 * "smart" indentation to make it easy to write Elm code, especially
 * since Elm's parser depends on indentation as an implicit delimiter.
 */
class ElmOnEnterIndentHandler : EnterHandlerDelegateAdapter() {

    override fun preprocessEnter(
            file: PsiFile,
            editor: Editor,
            caretOffsetRef: Ref<Int>,
            caretAdvanceRef: Ref<Int>,
            dataContext: DataContext,
            originalHandler: EditorActionHandler?
    ): Result {
        if (file !is ElmFile) return Result.Continue
        if (!CodeInsightSettings.getInstance()!!.SMART_INDENT_ON_ENTER) return Result.Continue


        // get current document and commit any changes, so we'll get latest PSI
        val document = editor.document
        PsiDocumentManager.getInstance(file.project).commitDocument(document)

        val caretOffset = caretOffsetRef.get()
        val text = document.charsSequence

        val isEOF = caretOffset == text.length
        val offset = if (isEOF) {
            caretOffset - 1
        } else {
            // skip following spaces and tabs
            CharArrayUtil.shiftForward(text, caretOffset, " \t")
        }

        // bail out if the caret is not at the end of the line/file
        val isEOL = isEOF || offset < text.length && text[offset] == '\n'
        if (!isEOL) {
            return Result.Continue
        }

        // find the non-whitespace PsiElement at the caret
        var elementAtCaret = file.findElementAt(offset)
                ?: return Result.Continue
        if (elementAtCaret.isEolWhitespace(offset) || elementAtCaret.isVirtualLayoutToken()) {
            elementAtCaret = PsiTreeUtil.prevVisibleLeaf(elementAtCaret)
                    ?: return Result.Continue
        }

        // bail out if the user pressed Enter after anything other than the special keywords
        val elementAtCaretType = elementAtCaret.elementType
        if (elementAtCaretType != ElmTypes.EQ
                && elementAtCaretType != ElmTypes.LET
                && elementAtCaretType != ElmTypes.OF
                && elementAtCaretType != ElmTypes.ARROW) {
            return Result.Continue
        }


        // bail out early in cases where we do NOT want to smart indent

        if (elementAtCaretType == ElmTypes.ARROW
                && elementAtCaret.parentOfType<ElmCaseOfBranch>() == null) {
            return Result.Continue
        }

        if (elementAtCaretType == ElmTypes.EQ
                && elementAtCaret.parentOfType(ElmRecordExpr::class.java, ElmTypeDeclaration::class.java) != null) {
            return Result.Continue
        }


        // IntelliJ will match leading whitespace of previous lines, so we only need
        // to indent one level, even for deeply nested let-in declarations.
        val textToInsert = file.indentStyle.oneLevelOfIndentation
        document.insertString(caretOffset, textToInsert)
        caretAdvanceRef.set(textToInsert.length)

        return Result.Default
    }

    // Returns true for
    //   ```
    //   foo  <caret>
    //
    //
    //   ```
    //
    // Returns false for
    //   ```
    //   foo
    //
    //   <caret>
    //   ```
    private fun PsiElement.isEolWhitespace(caretOffset: Int): Boolean {
        if (node?.elementType != TokenType.WHITE_SPACE) return false
        val pos = node.text.indexOf('\n')
        return pos == -1 || caretOffset <= pos + textRange.startOffset
    }
}
