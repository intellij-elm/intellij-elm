package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
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
class ElmLetIn(node: ASTNode) : ElmPsiElementImpl(node) {


    /**
     * The local declaration bindings.
     *
     * In a well-formed program, there will be at least one element.
     */
    val innerValueDeclarationList: List<ElmInnerValueDeclaration>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerValueDeclaration::class.java)


    /**
     * Optional type annotations on each local declaration
     */
    val innerTypeAnnotationList: List<ElmInnerTypeAnnotation>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerTypeAnnotation::class.java)


    /**
     * The body expression.
     *
     * In a well-formed program, this will be non-null
     */
    val expression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)

}
