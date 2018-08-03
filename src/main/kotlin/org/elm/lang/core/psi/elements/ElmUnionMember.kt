package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.stubs.ElmUnionMemberStub


class ElmUnionMember : ElmStubbedNamedElementImpl<ElmUnionMemberStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmUnionMemberStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /** the union tag/constructor */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /**
     * All parameters of the member, if any.
     *
     * The elements will be in source order, and will be any of the following types:
     *
     * [ElmTypeVariableRef], [ElmRecordType], [ElmTupleType], [ElmTypeRef]
     */
    val allParameters: Sequence<ElmPsiElement>
        get() = directChildren.filterIsInstance<ElmPsiElement>().filter {
            it is ElmTypeVariableRef
                    || it is ElmRecordType
                    || it is ElmTupleType
                    || it is ElmTypeRef
        }
}
