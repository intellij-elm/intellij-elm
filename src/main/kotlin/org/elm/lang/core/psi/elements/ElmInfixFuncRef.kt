package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference
import org.elm.lang.core.stubs.ElmPlaceholderRefStub

/**
 * A reference to the function that implements an infix operator.
 */
class ElmInfixFuncRef : ElmStubbedElement<ElmPlaceholderRefStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderRefStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val referenceName: String
        get() = stub?.refName ?: referenceNameElement.text

    override fun getReference(): ElmReference =
            LocalTopLevelValueReference(this)
}