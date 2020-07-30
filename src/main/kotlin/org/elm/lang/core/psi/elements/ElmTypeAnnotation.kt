package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.stubDirectChildrenOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.LexicalValueReference
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference
import org.elm.lang.core.stubs.ElmPlaceholderRefStub


/**
 * A type annotation
 *
 * e.g. `length : String -> Int`
 *
 */
class ElmTypeAnnotation : ElmStubbedElement<ElmPlaceholderRefStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderRefStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    /**
     * The left-hand side of the type annotation which names the value
     *
     * e.g. `length` in `length : String -> Int`
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The right-hand side of the type annotation which describes the type of the value.
     *
     * e.g. `String -> Int` in `length : String -> Int`
     *
     * In a well-formed program, this will be non-null.
     */
    val typeExpression: ElmTypeExpression?
        get() = stubDirectChildrenOfType<ElmTypeExpression>().singleOrNull()


    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier

    override val referenceName: String
        get() = stub?.refName ?: referenceNameElement.text

    override fun getReference(): ElmReference =
            if (parent is PsiFile) LocalTopLevelValueReference(this)
            else LexicalValueReference(this)
}
