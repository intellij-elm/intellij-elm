package org.elm.ide.typing

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.elm.lang.core.psi.ElmTypes.*

// A QuoteHandler is called while the user is typing to control quote matching functionality
// A MultiCharQuoteHandler adds the ability to match quotes that are more than one character long.
class ElmQuoteHandler : QuoteHandler, MultiCharQuoteHandler {
    // If this returns true, the editor will move the carat one character right instead of inserting the typed
    // character
    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return when (iterator.tokenType) {
            CLOSE_QUOTE, CLOSE_CHAR -> true
            else -> false
        }
    }

    // If this returns true, the editor will insert a copy of the typed character after the carat
    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return when (iterator.tokenType) {
            OPEN_QUOTE, OPEN_CHAR -> offset == iterator.start
            else -> false
        }
    }

    // If this returns false, the editor won't insert a closing quote
    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        return true
    }

    // If this returns true, and the carat is immediately after an odd number of backslashes, the
    // editor won't perform the isOpeningQuote or isClosingQuote behavior. The character will just be typed normally.
    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {
        return when (iterator.tokenType) {
            REGULAR_STRING_PART, STRING_ESCAPE, INVALID_STRING_ESCAPE, OPEN_QUOTE, CLOSE_QUOTE -> true
            else -> false
        }
    }

    // Part of MultiCharQuoteHandler. If this returns non-null, it will be inserted after the carat.
    // This takes precedence over the isOpeningQuote behavior, but if it returns null,
    // isOpeningQuote will still be called.
    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? {
        return if (isOpeningTripleQuote(iterator, offset)) "\"\"\""
        else null
    }

    private fun isOpeningTripleQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val text = iterator.document.text
        val tokenType = iterator.tokenType
        return offset >= 3
                && text[offset - 1] == '"'
                && text[offset - 2] == '"'
                && text[offset - 3] == '"'
                && (tokenType == REGULAR_STRING_PART && offset == iterator.start // middle of file
                || tokenType == OPEN_QUOTE && offset == iterator.start + 3) // end of file
    }
}
