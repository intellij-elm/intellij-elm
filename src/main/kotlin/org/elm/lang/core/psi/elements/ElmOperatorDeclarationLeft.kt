
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmOperatorDeclarationLeftStub


/**
 * The declaration of an operator function
 *
 * e.g. `(=>) a b = (a, b)`
 *
 * @see [ElmFunctionDeclarationLeft]
 */
class ElmOperatorDeclarationLeft : ElmStubbedNamedElementImpl<ElmOperatorDeclarationLeftStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.OPERATOR)

    constructor(stub: ElmOperatorDeclarationLeftStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.OPERATOR)


    val operatorIdentifier: PsiElement
        get() = findNotNullChildByType(OPERATOR_IDENTIFIER)

    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

}
