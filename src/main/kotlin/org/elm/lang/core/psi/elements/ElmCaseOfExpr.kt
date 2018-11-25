package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * A pattern-matching expression.
 *
 * e.g. `case x of
 *          Just y -> y
 *          Nothing -> 0`
 */
class ElmCaseOfExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag {

    /**
     * The expression which the case-of performs pattern matching against.
     *
     * In a well-formed program, this will be non-null.
     */
    val expression: ElmExpressionTag?
        get() = findChildByClass(ElmExpressionTag::class.java)

    /**
     * The pattern-matching branches.
     *
     * In a well-formed program, there will be at least one branch.
     */
    val branches: List<ElmCaseOfBranch>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmCaseOfBranch::class.java)
}
