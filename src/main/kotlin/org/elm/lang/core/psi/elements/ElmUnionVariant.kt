package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.ElmUnionVariantParameterTag
import org.elm.lang.core.stubs.ElmUnionVariantStub

/**
 * One of the cases in a union type declaration
 *
 * e.g. `Just a` and `Nothing` in:
 * ```
 * type Maybe a
 *     = Just a
 *     | Nothing
 * ```
 */
class ElmUnionVariant : ElmStubbedNamedElementImpl<ElmUnionVariantStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmUnionVariantStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /** the variant name */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /** All parameters of the variant, if any. */
    val allParameters: Sequence<ElmUnionVariantParameterTag>
        get() = directChildren.filterIsInstance<ElmUnionVariantParameterTag>()
}
