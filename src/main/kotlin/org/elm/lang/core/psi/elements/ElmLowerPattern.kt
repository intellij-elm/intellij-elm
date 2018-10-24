package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*


/**
 * A lower-case identifier within a pattern
 *
 * e.g. `a` and `b` in the declaration `(a, b) = (0, 0)`
 */
class ElmLowerPattern(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.LOWER), ElmNameDeclarationPatternTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)
}
