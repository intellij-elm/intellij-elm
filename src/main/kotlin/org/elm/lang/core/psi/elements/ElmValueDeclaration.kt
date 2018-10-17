package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiComment
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmValueDeclarationStub


/**
 * A top-level value/function declaration.
 *
 * Most of the time this is a simple value or function declaration
 * e.g. `x = 42` or `f x = 2 * x`
 * That case is covered by [ElmFunctionDeclarationLeft].
 *
 * The other case, and it's quite rare, is when you are binding a value
 * to a pattern, possibly introducing multiple top-level names.
 * e.g. `(x,y) = (0,0)`
 */
class ElmValueDeclaration : ElmStubbedElement<ElmValueDeclarationStub>, ElmDocTarget {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmValueDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)

    val modificationTracker = SimpleModificationTracker()

    override fun getIcon(flags: Int) =
            ElmIcons.FUNCTION

    val functionDeclarationLeft: ElmFunctionDeclarationLeft?
        get() = PsiTreeUtil.getStubChildOfType(this, ElmFunctionDeclarationLeft::class.java)

    /** Warning: Elm 0.18 only! will always be null in 0.19 */
    // TODO [drop 0.18] remove this property
    val operatorDeclarationLeft: ElmOperatorDeclarationLeft?
        get() = PsiTreeUtil.getStubChildOfType(this, ElmOperatorDeclarationLeft::class.java)

    /** The pattern if this declaration is binding multiple names. */
    // In Elm 0.19, this is only valid inside a let block
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
            if (includeParameters)
                namedElements.addAll(functionDeclarationLeft!!.namedParameters)

        } else if (operatorDeclarationLeft != null) {
            // an operator declaration
            namedElements.add(operatorDeclarationLeft!!)
            if (includeParameters)
                namedElements.addAll(operatorDeclarationLeft!!.namedParameters)

        } else if (pattern != null) {
            // value destructuring (e.g. `(x,y) = (0,0)` in a let/in declaration)
            namedElements.addAll(pattern!!.descendantsOfType<ElmLowerPattern>())
        }

        return namedElements
    }

    /**
     * Names that are declared as parameters to a function
     */
    fun declaredParameters(): List<ElmNamedElement> {
        return if (functionDeclarationLeft != null) {
            functionDeclarationLeft!!.namedParameters
        } else if (operatorDeclarationLeft != null) {
            // TODO [drop 0.18] remove this case entirely
            operatorDeclarationLeft!!.namedParameters
        } else {
            emptyList()
        }
    }

    /** The type annotation for this function, or `null` if there isn't one. */
    val typeAnnotation: ElmTypeAnnotation?
        get() = prevSiblings.withoutWsOrComments.firstOrNull() as? ElmTypeAnnotation

    override val docComment: PsiComment?
        get() = (prevSiblings.withoutWs.filter { it !is ElmTypeAnnotation }.firstOrNull() as? PsiComment)
                ?.takeIf { it.text.startsWith("{-|") }
}
