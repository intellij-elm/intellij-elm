package org.elm.lang.core.diagnostics

import com.intellij.psi.PsiElement
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
        get() =
            if (expected == 0) "This value is not a function, but it was given $actual ${pl(actual, "argument")}."
            else "The function expects $expected ${pl(expected, "argument")}, but it got $actual instead."
}

class RedefinitionError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Conflicting name declaration"
}

class TypeMismatchError(
        element: PsiElement,
        private val actual: Ty,
        private val expected: Ty
) : ElmDiagnostic(element) {
    override val message: String
        get() {
            var expectedRendered = expected.renderedText(false, false)
            var foundRendered = actual.renderedText(false, false)

            if (expectedRendered == foundRendered) {
                expectedRendered = expected.renderedText(false, true)
                foundRendered = actual.renderedText(false, true)
            }
            return "Type mismatch." +
                    "<br>Required: $expectedRendered" +
                    "<br>Found: $foundRendered"
        }
}

private fun pl(n: Int, singular: String, plural: String = singular + "s") = if (n == 1) singular else plural
