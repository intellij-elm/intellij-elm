package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * A let-in expression
 *
 * For example:
 *  ```
 *  let
 *      x = 320
 *      y = 480
 *  in
 *      x + y
 *  ```
 */
class ElmLetInExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag {
    /**
     * The local declaration bindings.
     *
     * In a well-formed program, there will be at least one element.
     */
    val valueDeclarationList: List<ElmValueDeclaration>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmValueDeclaration::class.java)

    /**
     * The body expression.
     *
     * In a well-formed program, this will be non-null
     */
    val expression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)
}
