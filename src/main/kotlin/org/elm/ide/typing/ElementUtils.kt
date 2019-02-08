package org.elm.ide.typing

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.*

/** Return the string that should indent an element based on the number of ancestors it has */
fun guessIndent(element: PsiElement, offset: Int = 0): String {
    var current: PsiElement? = element
    var count = 0

    while (current != null && current !is PsiFile) {
        val next = current.parent
        when (next) {
            is ElmCaseOfExpr, is ElmCaseOfBranch,
            is ElmValueDeclaration, is ElmIfElseExpr -> count += 1
            // the expression part of a let-in is at the same indentation as its parent
            is ElmLetInExpr -> if (next.expression != current) count += 1
        }

        current = next
    }

    return "    ".repeat(count + offset)
}
