package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*


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
    val expression: ElmExpressionTag?
        get() = findChildByClass(ElmExpressionTag::class.java)

    /** The `let` element. In a well formed program, this will not return null */
    val letKeyword: PsiElement get() = findNotNullChildByType(ElmTypes.LET)

    /** The `in` element. In a well formed program, this will not return null */
    val inKeyword: PsiElement?
        get() = findChildByType(ElmTypes.IN) ?:
        // If there's no valueDeclarationList, the in keyword is enclosed in an error element
        (lastChild as? PsiErrorElement)?.firstChild?.takeIf { it.elementType == ElmTypes.IN }
}
