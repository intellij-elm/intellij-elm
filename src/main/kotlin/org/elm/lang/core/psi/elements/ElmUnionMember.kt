package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmUnionMemberStub


class ElmUnionMember : ElmStubbedNamedElementImpl<ElmUnionMemberStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmUnionMemberStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /** the union tag/constructor */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    val typeVariableRefList: List<ElmTypeVariableRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeVariableRef::class.java)

    val recordTypeList: List<ElmRecordType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecordType::class.java)

    val tupleTypeList: List<ElmTupleType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTupleType::class.java)

    val typeRefList: List<ElmTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeRef::class.java)

}
