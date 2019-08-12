package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ExposedUnionConstructorReference
import org.elm.lang.core.stubs.ElmPlaceholderRefStub


/**
 * A union constructor explicitly exposed from an exposing list
 *
 * e.g. `Home` in `import App exposing Page(Home)`
 */
class ElmExposedUnionConstructor : ElmStubbedElement<ElmPlaceholderRefStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderRefStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = upperCaseIdentifier

    override val referenceName: String
        get() = stub?.refName ?: referenceNameElement.text

    override fun getReference() =
            ExposedUnionConstructorReference(this)
}
