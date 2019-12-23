package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase.UPPER
import org.elm.lang.core.stubs.ElmAsClauseStub


/**
 * Introduces an alias name for the imported module.
 *
 * e.g. the 'as U' in 'import Data.User as U'
 */
class ElmAsClause : ElmStubbedNamedElementImpl<ElmAsClauseStub> {

    constructor(node: ASTNode) :
            super(node, UPPER)

    constructor(stub: ElmAsClauseStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, UPPER)

    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)
}
