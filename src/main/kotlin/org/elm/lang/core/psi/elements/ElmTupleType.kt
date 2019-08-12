package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmPlaceholderStub


/**
 * A type expression for a tuple
 *
 * e.g. `(Int, String)` in a type declaration or annotation
 */
class ElmTupleType : ElmStubbedElement<ElmPlaceholderStub>,
        ElmUnionVariantParameterTag, ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val typeExpressionList: List<ElmTypeExpression>
        get() = stubDirectChildrenOfType()

    val unitExpr: ElmUnitExpr?
        get() = stubDirectChildrenOfType<ElmUnitExpr>().singleOrNull()

}
