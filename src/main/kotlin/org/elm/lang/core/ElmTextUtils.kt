package org.elm.lang.core

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.elements.ElmParenthesizedExpr
import org.elm.lang.core.psi.indentStyle
import org.elm.lang.core.psi.startOffset
import org.elm.utils.getIndent
import kotlin.math.ceil

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

val ElmPsiElement.withoutParens: ElmPsiElement get() { return unwrapParensHelp(this) }
val ElmParenthesizedExpr.withoutExtraParens: ElmParenthesizedExpr get() { return unwrapNestedParensHelp(this) }

val ElmParenthesizedExpr.comments : Sequence<PsiComment>
    get() {
    return directChildren.filterIsInstance<PsiComment>()
}

private fun unwrapParensHelp(expression: ElmPsiElement): ElmPsiElement {
    return when (expression) {
        is ElmParenthesizedExpr -> {
            val nestedExpression = expression.expression
            if (nestedExpression == null) {
               expression
            } else {
                if (expression.comments.count() == 0) {
                    unwrapParensHelp(nestedExpression)
                } else {
                    expression
                }
            }
        }
        else -> {
            expression
        }
    }
}

private fun unwrapNestedParensHelp(expression: ElmParenthesizedExpr): ElmParenthesizedExpr {
    val nestedExpression = expression.expression
    return if (nestedExpression == null) {
        expression
    } else if (nestedExpression is ElmParenthesizedExpr) {
        unwrapNestedParensHelp(nestedExpression)
    } else {
        expression
    }
}
/**
 * Build a string of indented text lines. Useful for multi-line code generation.
 *
 * @see buildIndentedText
 */
class IndentedTextBuilder(startLevel: Int, val indentSize: Int) {
    var level: Int = startLevel
    private var buffer = StringBuilder()

    fun appendLine(str: String = "") {
        if (str.isBlank()) {
            buffer.appendln()
            return
        }
        buffer.append(" ".repeat(level * indentSize))
        buffer.appendln(str)
    }

    fun build() = buffer.toString()
}

/**
 * Build a string of indented text that can be used to replace [element] in the source editor.
 *
 * The indent size is determined based on the user's preference in IntelliJ code style settings.
 *
 * @see ElmPsiElement.textWithNormalizedIndents
 */
fun buildIndentedText(element: ElmPsiElement, builder: (IndentedTextBuilder).() -> Unit): String {
    val doc = PsiDocumentManager.getInstance(element.project).getDocument(element.elmFile)
            ?: error("Failed to find document for $element")
    val existingIndent = doc.getIndent(element.startOffset)
    val indentSize = element.indentStyle.INDENT_SIZE
    val startLevel = ceil(existingIndent.length / indentSize.toDouble()).toInt()
    val b = IndentedTextBuilder(startLevel, indentSize)
    b.builder()
    return b.build()
}
