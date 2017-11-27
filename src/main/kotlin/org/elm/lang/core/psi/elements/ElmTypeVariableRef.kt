package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes

/**
 * Holds a lower-case identifier within a type reference which
 * gives the type variable in a parametric type.
 *
 * e.g. the 'a' in `map : (a -> b) -> List a -> List b`
 */
class ElmTypeVariableRef(node: ASTNode) : ElmPsiElementImpl(node) {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)
}