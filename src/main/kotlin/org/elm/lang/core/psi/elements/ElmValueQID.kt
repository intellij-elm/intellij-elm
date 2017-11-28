package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER

/**
 * A value identifier, possibly qualified by the module/alias that contains the value.
 *
 * It may consist of an optional module prefix, upper-case-identifiers,
 * followed by a single lower-case identifier.
 *
 * e.g. `Json.Decode.string`
 */
class ElmValueQID(node: ASTNode) : ElmPsiElementImpl(node), ElmQID {

    /**
     * The qualifiers which identify the module/alias, if any.
     */
    val upperCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(UPPER_CASE_IDENTIFIER)

    /**
     * The value identifier
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    val isQualified: Boolean
        get() = upperCaseIdentifierList.isNotEmpty()
}
