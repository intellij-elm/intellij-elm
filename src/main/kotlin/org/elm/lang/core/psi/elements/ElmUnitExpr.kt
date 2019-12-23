package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmPlaceholderStub


class ElmUnitExpr : ElmStubbedElement<ElmPlaceholderStub>,
        ElmAtomTag, ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)

}
