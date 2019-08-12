package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.IdentifierCase.LOWER
import org.elm.lang.core.stubs.ElmLowerTypeNameStub


class ElmLowerTypeName : ElmStubbedNamedElementImpl<ElmLowerTypeNameStub> {

    constructor(node: ASTNode) :
            super(node, LOWER)

    constructor(stub: ElmLowerTypeNameStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, LOWER)
}