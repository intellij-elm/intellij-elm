package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmInfixDeclarationStub

/**
 * A top-level declaration that describes the associativity and the precedence
 * of a binary operator.
 *
 * For example, `infix right 5 (++) = append` means that the operator `++` has right associativity
 * at precedence level of 5.
 *
 * As of Elm 0.19, these are now restricted to packages owned by elm-lang.
 */
class ElmInfixDeclaration : ElmStubbedNamedElementImpl<ElmInfixDeclarationStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.OPERATOR)

    constructor(stub: ElmInfixDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.OPERATOR)


    val precedence: PsiElement
        get() = findNotNullChildByType(ElmTypes.NUMBER_LITERAL)

    val operatorIdentifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.OPERATOR_IDENTIFIER)

    val valueExpr: ElmValueExpr?
        get() = findChildByType(ElmTypes.VALUE_EXPR)
}