package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase


/**
 * Introduces an alias name for the imported module.
 *
 * e.g. the 'as U' in 'import Data.User as U'
 */
class ElmAsClause(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.UPPER) {

    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)
}
