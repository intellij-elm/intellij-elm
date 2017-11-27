package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


/**
 * Accessing one or more fields on a base record.
 *
 * e.g. `model.currentUser.name`
 */
class ElmFieldAccess(node: ASTNode) : ElmPsiElementImpl(node) {

    val lowerCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(LOWER_CASE_IDENTIFIER)

    // TODO [kl] implement reference/resolve
}
