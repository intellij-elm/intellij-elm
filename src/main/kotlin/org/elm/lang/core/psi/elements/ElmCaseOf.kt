package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand


/**
 * A pattern-matching expression.
 *
 * e.g. `case x of
 *          Just y -> y
 *          Nothing -> 0`
 */
class ElmCaseOf(node: ASTNode) : ElmPsiElementImpl(node), ElmOperand {

    /**
     * The expression which the case-of performs pattern matching against.
     *
     * In a well-formed program, this will be non-null.
     */
    val expression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)


    /**
     * The pattern-matching branches.
     *
     * In a well-formed program, there will be at least one branch.
     */
    val branches: List<ElmCaseOfBranch>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmCaseOfBranch::class.java)

}
