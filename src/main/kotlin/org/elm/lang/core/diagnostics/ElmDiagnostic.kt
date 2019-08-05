package org.elm.lang.core.diagnostics

import com.intellij.psi.PsiElement
import org.elm.lang.core.types.*

sealed class ElmDiagnostic(val element: PsiElement, val endElement: PsiElement? = null) {
    abstract val message: String
}

class ArgumentCountError(
        element: PsiElement,
        endElement: PsiElement,
        private val actual: Int,
        private val expected: Int,
        private val isType: Boolean = false
) : ElmDiagnostic(element, endElement) {
    override val message: String
        get() =
            if (expected == 0 && !isType) "This value is not a function, but it was given $actual ${pl(actual, "argument")}."
            else "The ${if (isType) "type" else "function"} expects $expected ${pl(expected, "argument")}, but it got $actual instead."
}

class ParameterCountError(
        element: PsiElement,
        endElement: PsiElement,
        private val actual: Int,
        private val expected: Int
) : ElmDiagnostic(element, endElement) {
    override val message: String
        get() =
            "The function expects $expected ${pl(expected, "parameter")}, but it got $actual instead."
}

class RedefinitionError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Conflicting name declaration"
}

class PartialPatternError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Pattern does not cover all possibilities"
}

class BadRecursionError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Infinite recursion"
}

class CyclicDefinitionError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Value cannot be defined in terms of itself"
}

class InfiniteTypeError(
        element: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Infinite self-referential type"
}

class RecordFieldError(
        element: PsiElement,
        private val name: String
) : ElmDiagnostic(element) {
    override val message: String
        get() = "Record does not have field '$name'"
}

class RecordBaseIdError(
        element: PsiElement,
        private val actual: Ty
) : ElmDiagnostic(element) {
    override val message: String
        get() {
            val expectedRendered = actual.renderedText()
            return "Type must be a record.<br>Found: $expectedRendered"
        }
}

class FieldAccessOnNonRecordError(
        element: PsiElement,
        private val actual: Ty
) : ElmDiagnostic(element) {
    override val message: String
        get() {
            val expectedRendered = actual.renderedText()
            return "Value is not a record, cannot access fields.<br>Type: $expectedRendered"
        }
}

class NonAssociativeOperatorError(
        element: PsiElement,
        private val operator: PsiElement
) : ElmDiagnostic(element) {
    override val message: String
        get() {
            return "Operator (${operator.text}) is not associative, and so cannot be chained"
        }
}

class TypeMismatchError(
        element: PsiElement,
        private val actual: Ty,
        private val expected: Ty,
        endElement: PsiElement? = null,
        private val patternBinding: Boolean = false,
        private val recordDiff: RecordDiff? = null
) : ElmDiagnostic(element, endElement) {
    override val message: String
        get() {
            var expectedRendered = expected.renderedText()
            var foundRendered = actual.renderedText()

            if (expectedRendered == foundRendered || tysAreAmbiguousUnions(actual, expected)) {
                expectedRendered = expected.renderedText(linkify = false, withModule = true)
                foundRendered = actual.renderedText(linkify = false, withModule = true)
            }
            val message = if (patternBinding) {
                "Invalid pattern." +
                        "<br>Required type: $expectedRendered" +
                        "<br>Pattern type: $foundRendered"
            } else {
                "Type mismatch." +
                        "<br>Required: $expectedRendered" +
                        "<br>Found: $foundRendered"
            }
            return message + (recordDiff?.let { renderRecordDiff(it) } ?: "")
        }

    private fun tysAreAmbiguousUnions(l: Ty, r: Ty): Boolean {
        return l is TyUnion && r is TyUnion
                && l.name == r.name
                && l.module != r.module
    }

    private fun renderRecordDiff(recordDiff: RecordDiff) = buildString {
        if (recordDiff.extra.isNotEmpty()) {
            append("<br>Extra fields: ")
            append(TyRecord(fields = recordDiff.extra).renderedText())
        }
        if (recordDiff.missing.isNotEmpty()) {
            append("<br>Missing fields: ")
            append(TyRecord(fields = recordDiff.missing).renderedText())
        }
        if (recordDiff.mismatched.isNotEmpty()) {
            append("<br>Mismatched fields: ")
            for ((k, v) in recordDiff.mismatched) {
                append("<br>&nbsp;&nbsp;Field ").append(k).append(":")
                append("<br>&nbsp;&nbsp;&nbsp;&nbsp;Required: ")
                append(v.second.renderedText())
                append("<br>&nbsp;&nbsp;&nbsp;&nbsp;Found: ")
                append(v.first.renderedText())
            }
        }
    }
}

class TypeArgumentCountError(
        element: PsiElement,
        private val actual: Int,
        private val expected: Int
) : ElmDiagnostic(element, null) {
    override val message: String
        get() =
            "The type expects $expected ${pl(expected, "argument")}, but it got $actual instead."
}


private fun pl(n: Int, singular: String, plural: String = singular + "s") = if (n == 1) singular else plural
