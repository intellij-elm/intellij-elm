package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.IdentifierCase.LOWER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.TypeVariableReference
import org.elm.lang.core.stubs.ElmTypeVariableStub

/**
 * Holds a lower-case identifier within a type expression which
 * gives the type variable in a parametric type.
 *
 * e.g. the 'a' in `map : (a -> b) -> List a -> List b`
 * e.g. the last `a` in `type Foo a = Bar a`
 */
class ElmTypeVariable : ElmStubbedNamedElementImpl<ElmTypeVariableStub>,
        ElmReferenceElement, ElmUnionVariantParameterTag, ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    constructor(node: ASTNode) :
            super(node, LOWER)

    constructor(stub: ElmTypeVariableStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, LOWER)


    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    override val referenceNameElement: PsiElement
        get() = identifier

    override val referenceName: String
        get() = name // equivalent to `referenceNameElement.text` but this checks stub first

    override fun getReference() = TypeVariableReference(this)

    // type variables can only be referenced within the declaration they're declared in, and, in the
    // case of function annotations, from annotations of nested functions
    override fun getUseScope(): SearchScope {
        return LocalSearchScope(elmFile)
    }
}
