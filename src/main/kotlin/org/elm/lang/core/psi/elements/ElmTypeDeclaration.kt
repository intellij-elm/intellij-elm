package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.*
import org.elm.lang.core.stubs.ElmTypeDeclarationStub


/**
 * Declares a union type
 *
 * e.g. `type Page = Home | Login | NotFound`
 *
 * If the union type is parametric, [lowerTypeNameList] will be non-empty. An example
 * of a parametric type would be a binary tree: `type Tree a = Nil | Branch (Tree a) (Tree a)`.
 * In which case, [lowerTypeNameList] would contain a single element representing the
 * type variable `a`.
 */
class ElmTypeDeclaration : ElmStubbedNamedElementImpl<ElmTypeDeclarationStub>, ElmDocTarget, ElmExposableTag {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmTypeDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)

    override fun getIcon(flags: Int) =
            ElmIcons.UNION_TYPE

    /**
     * Zero-or-more parametric type variables which may appear in the union variants.
     */
    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = stubDirectChildrenOfType()

    /**
     * The union variants which define the structure of the type.
     *
     * In a well-formed program, this will contain at least one element.
     */
    val unionVariantList: List<ElmUnionVariant>
        get() = stubDirectChildrenOfType()
}
