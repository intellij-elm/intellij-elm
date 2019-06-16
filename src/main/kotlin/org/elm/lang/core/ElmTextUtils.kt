package org.elm.lang.core

import com.intellij.openapi.util.text.StringUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.startOffset

/**
 * Convert a string so that its first character is guaranteed to be lowercase.
 * This is necessary in some parts of Elm's syntax (e.g. a function parameter).
 *
 * If the receiver consists of all uppercase letters, the entire thing will be made
 * lowercase (because "uuid" is a far more sensible transformation of "UUID" than "uUID").
 */
fun String.toElmLowerId(): String =
        when {
            isEmpty() -> ""
            all { it.isUpperCase() } -> toLowerCase()
            else -> first().toLowerCase() + substring(1)
        }

/**
 * Returns the element's text content where each line has been normalized such that:
 *
 *   1) it starts with a non-whitespace character
 *   2) relative indentation is preserved
 *
 * This is useful when manually building strings involving multi-line Elm expressions and declarations.
 */
val ElmPsiElement.textWithNormalizedIndents: String
    get() {
        val firstColumn = StringUtil.offsetToLineColumn(this.containingFile.text, this.startOffset).column
        return this.text.lines().mapIndexed { index: Int, s: String ->
            if (index == 0) s else s.drop(firstColumn)
        }.joinToString("\n")
    }