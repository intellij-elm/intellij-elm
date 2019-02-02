package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmNameDeclarationPatternTag
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmValueAssigneeTag
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.isTopLevel
import org.elm.lang.core.stubs.ElmFunctionDeclarationLeftStub


/**
 * The left-hand side of a value or function declaration.
 *
 * Examples:
 *
 * A simple value:
 * `foo = 42`
 *
 * A function that takes 2 arguments:
 * `update msg model = model`
 *
 * A declaration inside a `let/in` expression:
 * `let
 *      foo = 42
 *  in ...`
 */
class ElmFunctionDeclarationLeft : ElmStubbedNamedElementImpl<ElmFunctionDeclarationLeftStub>, ElmExposableTag, ElmValueAssigneeTag {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.LOWER)

    constructor(stub: ElmFunctionDeclarationLeftStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.LOWER)


    /**
     * The name given to the function/value
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * Zero or more parameters to the function
     */
    val patterns: Sequence<ElmFunctionParamTag>
        get() = directChildren.filterIsInstance<ElmFunctionParamTag>()


    /**
     * All parameter names declared in this function.
     *
     * e.g. `a`, `b`, `c`, `d`, and `e` in `foo a (b, (c, d)) {e} = 42`
     */
    val namedParameters: List<ElmNameDeclarationPatternTag>
        get() = PsiTreeUtil.collectElementsOfType(this, ElmNameDeclarationPatternTag::class.java).toList()


    /** Return true if this declaration is not nested in a let-in expression */
    val isTopLevel: Boolean
        get() {
            val p = parent
            return p is ElmValueDeclaration && p.isTopLevel
        }
}
