/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.ide.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*

/**
 * Find an [ElmExpressionTag] element at [offset], being smart about whether
 * we look to the left or the right of the caret offset.
 */
fun findExpressionAtCaret(file: ElmFile, offset: Int): ElmExpressionTag? {
    val expr = file.expressionAtOffset(offset)
    val exprBefore = file.expressionAtOffset(offset - 1)
    return when {
        expr == null -> exprBefore
        exprBefore == null -> expr
        PsiTreeUtil.isAncestor(expr, exprBefore, false) -> exprBefore
        else -> expr
    }
}

private fun ElmFile.expressionAtOffset(offset: Int): ElmExpressionTag? =
        findElementAt(offset)?.parentOfType(strict = false)

/**
 * Finds top-most [ElmExpressionTag] within selected range.
 */
fun findExpressionInRange(file: PsiFile, startOffset: Int, endOffset: Int): ElmExpressionTag? {
    val (element1, element2) = file.getElementRange(startOffset, endOffset) ?: return null

    // Get common expression parent.
    var parent = PsiTreeUtil.findCommonParent(element1, element2) ?: return null
    parent = parent.parentOfType<ElmExpressionTag>(strict = false) ?: return null

    // If our parent's deepest first child is element1 and deepest last - element 2,
    // then it is completely within selection, so this is our sought expression.
    if (element1 == PsiTreeUtil.getDeepestFirst(parent) && element2 == PsiTreeUtil.getDeepestLast(element2)) {
        return parent
    }

    return null
}

/**
 * Finds two edge leaf PSI elements within given range.
 */
fun PsiFile.getElementRange(startOffset: Int, endOffset: Int): Pair<PsiElement, PsiElement>? {
    val element1 = findElementAtIgnoreWhitespaceBefore(startOffset) ?: return null
    val element2 = findElementAtIgnoreWhitespaceAfter(endOffset - 1) ?: return null

    // Elements have crossed (for instance when selection was inside single whitespace block)
    if (element1.startOffset >= element2.endOffset) return null

    return element1 to element2
}

/**
 * Finds a leaf PSI element at the specified offset from the start of the text range of this node.
 * If found element is whitespace, returns its next non-whitespace sibling.
 */
fun PsiFile.findElementAtIgnoreWhitespaceBefore(offset: Int): PsiElement? {
    val element = findElementAt(offset)
    if (element is PsiWhiteSpace) {
        return findElementAt(element.getTextRange().endOffset)
    }
    return element
}

/**
 * Finds a leaf PSI element at the specified offset from the start of the text range of this node.
 * If found element is whitespace, returns its previous non-whitespace sibling.
 */
fun PsiFile.findElementAtIgnoreWhitespaceAfter(offset: Int): PsiElement? {
    val element = findElementAt(offset)
    if (element is PsiWhiteSpace) {
        return findElementAt(element.getTextRange().startOffset - 1)
    }
    return element
}