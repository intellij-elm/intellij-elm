package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmPlaceholderStub


/**
 * A record type definition
 *
 * e.g. { name : String, age : Int }
 */
class ElmRecordType : ElmStubbedElement<ElmPlaceholderStub>,
        ElmUnionVariantParameterTag, ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    /**
     * The type variable representing a generic record which this
     * definition extends.
     *
     * e.g. entity in `{ entity | vx : Float, vy: Float }`
     */
    val baseTypeIdentifier: ElmRecordBaseIdentifier?
        get() = stubDirectChildrenOfType<ElmRecordBaseIdentifier>().singleOrNull()

    /**
     * The definition of the fields which comprise the record proper.
     */
    val fieldTypeList: List<ElmFieldType>
        get() = stubDirectChildrenOfType()

}
