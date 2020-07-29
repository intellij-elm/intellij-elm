package org.elm.lang.core.psi

import com.intellij.psi.PsiElement

/**
 * Marker interface for Qualified ID elements (QIDs)
 *
 * A qualified id is a value or type identifier that may include a module/alias prefix.
 *
 * e.g. `List.length [1,2,3]` refers to the `length` value in the `List` module.
 */
interface ElmQID : ElmPsiElement {
    val upperCaseIdentifierList: List<PsiElement>

    /**
     * The upper-case identifiers (if any) that qualify this identifier.
     *
     * e.g. `Json` and `Decode` in the expression `Json.Decode.maybe`
     */
    val qualifiers: List<PsiElement>

    /**
     * The text of the span of elements that make up [qualifiers]
     *
     * e.g. `Json.Decode` in the expression `Json.Decode.maybe`
     */
    val qualifierPrefix: String

    /** Returns true if the qualified ID refers to Elm's "Kernel" modules,
     * which are defined in Javascript. This is useful since we don't (currently)
     * support PsiReferences between JS and Elm
     */
    val isKernelModule: Boolean
        get() = text.startsWith("Elm.Kernel.")

    val isQualified: Boolean
}
