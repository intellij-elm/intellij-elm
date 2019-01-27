package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*


/**
 * A lower-case identifier within a pattern
 *
 * e.g. the `x` parameter in `f x = 0`
 * e.g. `a` and `b` in the declaration `(a, b) = (0, 0)`
 */
class ElmLowerPattern(node: ASTNode) : ElmNamedElementImpl(node, IdentifierCase.LOWER), ElmNameDeclarationPatternTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    override fun getUseScope(): SearchScope {
        /**
         * Performance Optimization:
         *
         * Users frequently rename function parameters. And those parameter
         * names are generally very short (e.g. `x`) and/or very common (e.g. `index`).
         * IntelliJ's rename refactoring is built on top of "Find Usages" which must
         * first search for all strings that match and then resolve each reference
         * to see if it actually points at the thing that is being renamed.
         *
         * The string search is pretty fast since it uses an index, but resolving
         * each possible reference can be slow. In the case of function parameters,
         * their usages are constrained to just the body of the function. So we can
         * drastically reduce the number of candidates by returning a narrower
         * `SearchScope` in this case.
         */
        val owner = PsiTreeUtil.getParentOfType(this, ElmValueDeclaration::class.java)
                ?: return super.getUseScope()

        return LocalSearchScope(owner)
    }
}
