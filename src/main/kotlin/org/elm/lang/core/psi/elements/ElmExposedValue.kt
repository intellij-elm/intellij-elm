package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ExposedValueImportReference
import org.elm.lang.core.resolve.reference.ExposedValueModuleReference
import org.elm.lang.core.stubs.ElmPlaceholderRefStub


/**
 * A value or function exposed via an import clause's `exposing` list.
 *
 * e.g. `bar` in `import Foo exposing (bar)`
 */
class ElmExposedValue : ElmStubbedElement<ElmPlaceholderRefStub>, ElmReferenceElement, ElmExposedItemTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderRefStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier

    override val referenceName: String
        get() = stub?.refName ?: referenceNameElement.text

    override fun getReference(): ElmReference =
            if (parentOfType<ElmModuleDeclaration>() != null)
                ExposedValueModuleReference(this)
            else
                ExposedValueImportReference(this)
}
