package org.elm.lang.core.psi

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmUpperCaseQID

/**
 * Marker interface for Qualified ID elements (QIDs)
 *
 * A qualified id is a value or type identifier that may include a module/alias prefix.
 *
 * e.g. `List.length [1,2,3]` refers to the `length` value in the `List` module.
 */
interface ElmQID : ElmPsiElement {
    val upperCaseIdentifierList: List<PsiElement>

    val qualifierPrefix: String
        get() {
            val frontParts = if (this is ElmUpperCaseQID)
                upperCaseIdentifierList.dropLast(1)
            else
                upperCaseIdentifierList
            return frontParts.joinToString(".") { it.text }
        }

    /** Returns true if the qualified ID refers to Elm's "Kernel" modules,
     * which are defined in Javascript. This is useful since we don't (currently)
     * support PsiReferences between JS and Elm
     */
    val isKernelModule: Boolean
        get() {
            val moduleName = upperCaseIdentifierList.joinToString(".") { it.text }
            return moduleName.startsWith("Elm.Kernel.")
                    || moduleName.startsWith("Native.") // TODO [drop 0.18] remove the "Native" clause
        }
}
