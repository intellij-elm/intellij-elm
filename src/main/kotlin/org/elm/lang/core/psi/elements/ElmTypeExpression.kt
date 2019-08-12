package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmPlaceholderStub


/**
 * A type expression.
 *
 * e.g.
 *
 *  - `Float`
 *  - `Maybe a`
 *  - `Int -> String`
 *  - `a -> (a -> {a: String})`
 */
class ElmTypeExpression : ElmStubbedElement<ElmPlaceholderStub>,
        ElmUnionVariantParameterTag, ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)

    /**
     * All segments of the type expression.
     *
     * The segments will be in source order. If this element is not a function, there will be one segment in
     * well-formed programs. For functions, there will be one segment per function argument, plus the return type.
     *
     * e.g. `Int` and `String` in `Int -> String`
     */
    val allSegments: List<ElmTypeExpressionSegmentTag>
        get() = stubDirectChildrenOfType()

    val allTypeVariablesRecursively: Collection<ElmTypeVariable>
        get() = stubDescendantsOfTypeStrict()
}
