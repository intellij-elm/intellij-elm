package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase.LOWER
import org.elm.lang.core.psi.stubDirectChildrenOfType
import org.elm.lang.core.stubs.ElmFieldTypeStub


/**
 * The definition of a record field's type.
 *
 * e.g. `name : String` in the record definition `type alias Person = { name : String }`
 */
class ElmFieldType : ElmStubbedNamedElementImpl<ElmFieldTypeStub> {

    constructor(node: ASTNode) :
            super(node, LOWER)

    constructor(stub: ElmFieldTypeStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, LOWER)


    /**
     * The name of a field in a record literal type definition
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The definition of the type of the field.
     */
    val typeExpression: ElmTypeExpression
        get() = stubDirectChildrenOfType<ElmTypeExpression>().single()

}
