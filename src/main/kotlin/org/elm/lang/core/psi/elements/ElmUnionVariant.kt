package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
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
// TODO [drop 0.18] in Elm 0.19, a union variant is not exposable directly
//      so we MIGHT want to stop implementing the `ElmExposableTag` interface
//      once we remove support for Elm 0.18. Or maybe there's a better way?
class ElmUnionVariant : ElmStubbedNamedElementImpl<ElmUnionVariantStub>, ElmExposableTag {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmUnionVariantStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /** the variant name */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /** All parameters of the variant, if any. */
    val allParameters: List<ElmUnionVariantParameterTag>
        get() = stubDirectChildrenOfType()
}
