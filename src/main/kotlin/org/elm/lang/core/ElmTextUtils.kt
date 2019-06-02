package org.elm.lang.core

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