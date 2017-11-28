package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER

/**
 * An identifier that refers to a Module, Union Constructor, or Record Constructor,
 * and it may contain an additional qualifier prefix which identifies the module/alias
 * from which the identifier may be obtained.
 */
class ElmUpperCaseQID(node: ASTNode) : ElmPsiElementImpl(node), ElmQID {

    /**
     * Guaranteed to contain at least one element
     */
    val upperCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(UPPER_CASE_IDENTIFIER)

    /**
     * True if the identifier is qualified by a module name (in the case of union or
     * record constructors) or the module exists in a hierarchy (in the case of a pure
     * module name in a module decl or import decl).
     *
     * TODO [kl] this double-duty is a bit strange. Maybe make a separate Psi element?
     */
    val isQualified: Boolean
        get() = upperCaseIdentifierList.size > 1
}
