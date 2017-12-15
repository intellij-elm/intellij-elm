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
interface ElmQID: ElmPsiElement {
    val upperCaseIdentifierList: List<PsiElement>

    val qualifierPrefix: String
        get() {
            val frontParts = if (this is ElmUpperCaseQID)
                                upperCaseIdentifierList.dropLast(1)
                             else
                                upperCaseIdentifierList
            return frontParts.joinToString(".") { it.text }
        }
}
