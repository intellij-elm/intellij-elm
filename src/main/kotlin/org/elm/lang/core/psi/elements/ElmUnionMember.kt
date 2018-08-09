package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.tags.ElmUnionMemberParameter
import org.elm.lang.core.stubs.ElmUnionMemberStub


class ElmUnionMember : ElmStubbedNamedElementImpl<ElmUnionMemberStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmUnionMemberStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /** the union tag/constructor */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /** All parameters of the member, if any. */
    val allParameters: Sequence<ElmUnionMemberParameter>
        get() = directChildren.filterIsInstance<ElmUnionMemberParameter>()
}
