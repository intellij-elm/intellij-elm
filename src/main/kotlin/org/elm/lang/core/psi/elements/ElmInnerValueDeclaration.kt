package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * A value declaration inside a `let/in` expression
 */
class ElmInnerValueDeclaration(node: ASTNode) : ElmPsiElementImpl(node) {

    val functionDeclarationLeft: ElmFunctionDeclarationLeft?
        get() = findChildByClass(ElmFunctionDeclarationLeft::class.java)

    val pattern: ElmPattern?
        get() = findChildByClass(ElmPattern::class.java)

    /**
     * The body expression on the right-hand-side
     *
     * In a well-formed program, this will be non-null
     */
    val expression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)


    // TODO [kl] duplicates code in ElmValueDeclaration. Maybe we can get rid
    // of the inner decl Psi element completely?

    /**
     * Names that are declared on the left-hand side of the equals sign
     * in a value declaration, including the name of the function/value itself
     * as well as destructured names caused by pattern matching.
     *
     * @param includeParameters include names declared as parameters to the function
     *                          (also includes destructured names). The default is `true`
     */
    fun declaredNames(includeParameters: Boolean = true): List<ElmNamedElement> {
        val namedElements = mutableListOf<ElmNamedElement>()

        if (functionDeclarationLeft != null) {
            // the most common case, a named function or value declaration
            namedElements.add(functionDeclarationLeft!!)

            if (includeParameters) {
                // add parameters, including destructured names
                namedElements.addAll(PsiTreeUtil.collectElementsOfType(functionDeclarationLeft, ElmLowerPattern::class.java))
                namedElements.addAll(PsiTreeUtil.collectElementsOfType(functionDeclarationLeft, ElmPatternAs::class.java))
            }
        } else if (pattern != null) {
            // value destructuring (e.g. `(x,y) = (0,0)` in a let/in declaration)
            namedElements.addAll(PsiTreeUtil.collectElementsOfType(pattern, ElmLowerPattern::class.java))
        }

        return namedElements
    }
}
