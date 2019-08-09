package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.psi.stubDirectChildrenOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.LexicalValueReference
import org.elm.lang.core.resolve.reference.LocalTopLevelValueReference
import org.elm.lang.core.stubs.ElmTypeAnnotationStub


/**
 * A type annotation
 *
 * e.g. `length : String -> Int`
 *
 * Either [lowerCaseIdentifier] or [operatorIdentifier] is non-null
 */
class ElmTypeAnnotation : ElmStubbedElement<ElmTypeAnnotationStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmTypeAnnotationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    /**
     * The left-hand side of the type annotation which names the value
     *
     * e.g. `length` in `length : String -> Int`
     */
    val lowerCaseIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The left-hand side when the value is a binary operator instead of a normal identifier.
     *
     * e.g. `(++)` in `(++) : String -> String -> String`
     */
    // TODO [drop 0.18] remove this property (and make lowerCaseIdentifier return non-null!)
    val operatorIdentifier: PsiElement?
        get() = findChildByType(OPERATOR_IDENTIFIER)

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
                ?: operatorIdentifier
                ?: throw RuntimeException("cannot determine type annotations's ref name element")

    override val referenceName: String
        get() = getStub()?.refName ?: referenceNameElement.text

    override fun getReference(): ElmReference =
            if (parent is PsiFile) LocalTopLevelValueReference(this)
            else LexicalValueReference(this)
}
