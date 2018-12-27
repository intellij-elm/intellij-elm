package org.elm.ide.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.parentOfType

/**
 * Adjusts the 'extend selection' behavior for [ElmValueDeclaration] and [ElmTypeAnnotation] so that they
 * mutually extend the selection to include the other.
 */
class ElmDeclAnnotationSelectionHandler : ExtendWordSelectionHandlerBase() {

    /*
        The plugin mechanism for refining the default 'extend selection' behavior is poor.

        Based on tracing the code, here is my understanding of how the internal logic works:

        It all starts in `SelectWordHandler.doExecute`. Starting with the element at the caret,
        it creates a selection range that just covers the caret position itself. And it defines
        an initial minimum range which spans the entire document. It then asks each registered
        `ExtendWordSelectionHandler` plugin extension point if it can select the thing and if so,
        to return a list of text ranges describing what it would like to select.

        Each candidate range is then checked to see if:

            (a) it would expand the current selection range
            (b) it is smaller than any candidate range seen so far

        If both conditions pass, the remaining candidates are ignored and IntelliJ will select
        the text in the editor described by the candidate that succeeded. Otherwise, the algorithm
        will walk the Psi tree upwards until it finds a larger selection.

        In terms of how this affects plugin authors, it appears that the following guidelines
        should be followed:

            (1) if you want to return a *larger* selection than would normally be returned for
                the given element, then you must call `ExtendWordSelectionHandlerBase.expandToWholeLine`
                with your desired range and return the resulting list of ranges. I have no idea why
                this is, but it appears to work.
            (2) if you want to return a *smaller* selection than would normally be returned for
                the given element, then you can just return the desired range directly.

     */

    override fun canSelect(e: PsiElement): Boolean =
            e is ElmValueDeclaration || e is ElmTypeAnnotation

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        when (e) {
            is ElmValueDeclaration -> {
                // extend the selection so that it also includes the preceding type annotation
                val typeAnnotation = e.typeAnnotation ?: return null
                val range = TextRange(typeAnnotation.textRange.startOffset, e.textRange.endOffset)
                return expandToWholeLine(editorText, range)
            }
            is ElmTypeAnnotation -> {
                // extend the selection so that it also includes the function body
                val targetDecl = e.reference.resolve() ?: return null
                val valueDecl = targetDecl.parentOfType<ElmValueDeclaration>()!!
                val range = TextRange(e.textRange.startOffset, valueDecl.textRange.endOffset)
                return expandToWholeLine(editorText, range)
            }
            else -> return null
        }
    }
}