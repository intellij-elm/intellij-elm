
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmTypeDeclarationStub


class ElmTypeDeclaration : ElmStubbedNamedElementImpl<ElmTypeDeclarationStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmTypeDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerTypeName::class.java)

    val unionMemberList: List<ElmUnionMember>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnionMember::class.java)

}
