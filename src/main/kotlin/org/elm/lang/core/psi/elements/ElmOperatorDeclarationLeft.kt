
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * The declaration of an operator function
 *
 * e.g. `(=>) a b = (a, b)`
 *
 * @see [ElmFunctionDeclarationLeft]
 */
class ElmOperatorDeclarationLeft(node: ASTNode) : ElmPsiElementImpl(node) {

    val operatorAsFunction: ElmOperatorAsFunction
        get() = findNotNullChildByClass(ElmOperatorAsFunction::class.java)

    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

}
