
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.stubs.ElmValueDeclarationStub


class ElmValueDeclaration : ElmStubbedElement<ElmValueDeclarationStub> {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmValueDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val functionDeclarationLeft: ElmFunctionDeclarationLeft?
        get() = PsiTreeUtil.getStubChildOfType(this, ElmFunctionDeclarationLeft::class.java)

    val operatorDeclarationLeft: ElmOperatorDeclarationLeft?
        get() = PsiTreeUtil.getStubChildOfType(this, ElmOperatorDeclarationLeft::class.java)

    val pattern: ElmPattern?
        get() = findChildByClass(ElmPattern::class.java)


    /**
     * The 'body' of the declaration. This is the right-hand side which is bound
     * to the name(s) on the left-hand side.
     *
     * In a well-formed program, this will be non-null.
     */
    val expression: ElmExpression?
        get() = findChildByClass(ElmExpression::class.java)

    /**
     * Names that are declared on the left-hand side of the equals sign in a value
     * declaration. In the basic case, this is the name of the function/value itself.
     * Optionally may also include "parameters" to the function. Parameters are simple,
     * lower-case identifiers like you would normally expect in a function, but also
     * any destructured names caused by pattern matching.
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
                extractDestructuredParameters(functionDeclarationLeft!!, namedElements)
            }
        } else if (operatorDeclarationLeft != null) {
            // an operator declaration
            namedElements.add(operatorDeclarationLeft!!)
            if (includeParameters) {
                extractDestructuredParameters(operatorDeclarationLeft!!, namedElements)
            }
        } else if (pattern != null) {
            // value destructuring (e.g. `(x,y) = (0,0)` in a let/in declaration)
            namedElements.addAll(PsiTreeUtil.collectElementsOfType(pattern, ElmLowerPattern::class.java))
        }

        return namedElements
    }

    private fun extractDestructuredParameters(parentElement: PsiElement, results: MutableList<ElmNamedElement>) {
        results.addAll(PsiTreeUtil.collectElementsOfType(parentElement, ElmLowerPattern::class.java))
        results.addAll(PsiTreeUtil.collectElementsOfType(parentElement, ElmPatternAs::class.java))
    }
}
