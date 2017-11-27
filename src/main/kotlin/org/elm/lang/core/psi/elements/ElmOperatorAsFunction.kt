
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes


class ElmOperatorAsFunction(node: ASTNode) : ElmPsiElementImpl(node) {

    val operator: PsiElement?
        get() = findChildByType<PsiElement>(TokenSet.create(
                ElmTypes.OPERATOR,
                ElmTypes.LIST_CONSTRUCTOR,
                ElmTypes.DOT,
                ElmTypes.MINUS))

}