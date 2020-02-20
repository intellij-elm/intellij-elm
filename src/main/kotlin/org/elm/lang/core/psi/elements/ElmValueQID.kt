package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ElmTypes.*
import kotlin.math.max

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
    override val upperCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(UPPER_CASE_IDENTIFIER)

    override val qualifiers: List<PsiElement>
        get() = upperCaseIdentifierList

    override val qualifierPrefix: String
        get() = text.let { it.take(max(0, it.lastIndexOf('.'))) }

    /**
     * The value identifier
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val isQualified: Boolean
        get() = findChildByType<PsiElement>(DOT) != null
}
