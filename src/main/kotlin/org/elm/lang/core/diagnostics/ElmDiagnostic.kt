package org.elm.lang.core.diagnostics

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmFunctionCall
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.renderedText

sealed class ElmDiagnostic(val element: PsiElement, val endElement: PsiElement? = null) {
    abstract val message: String
}

class TooManyArgumentsError(
        element: PsiElement,
        private val actual: Int,
        private val expected: Int
) : ElmDiagnostic(element) {
    override val message: String
        get() {
            if (expected == 0) {
                val name = (element as? ElmFunctionCall)?.function?.text ?: ""
                return "The `$name` value is not a function, but it was given $actual ${pl(actual, "argument")}."
            }


            val desc = if (element is ElmFunctionCall) {
                element.function?.let { "`${it.text}` function" }
                        ?: element.operator?.let { "${it.text} operator" }
                        ?: "function"
            } else "function"

            return "The $desc expects $expected ${pl(expected, "argument")}, but it got $actual instead."
        }
}

class TypeMismatchError(
        element: PsiElement,
        private val actual: Ty,
        private val expected: Ty
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Type mismatch.<br>Required: ${expected.renderedText(false)}<br>Found: ${actual.renderedText(false)}"
}

private fun pl(n: Int, singular: String, plural: String = singular + "s") = if (n == 1) singular else plural
