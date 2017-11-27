package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


/**
 * Gives a name to the value which is being destructured.
 *
 * e.g. `person` in `viewPerson ({name, age} as person) = ...`
 */
class ElmPatternAs(node: ASTNode) : ElmPsiElementImpl(node) {

    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

}