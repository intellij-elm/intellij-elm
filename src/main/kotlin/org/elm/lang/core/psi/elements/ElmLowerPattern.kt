package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*


// It's purpose is just to hold a lower-case identifier within a pattern
// e.g. the 'a' and `b` in the expression `let (a, b) = (0, 0) in a + b`
class ElmLowerPattern(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.LOWER),
        ElmConsPatternChildTag, ElmNameDeclarationPatternTag, ElmFunctionParamTag, ElmPatternChildTag,
        ElmUnionPatternChildTag {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)
}
