
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.IdentifierCase
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
class ElmTypeDeclaration : ElmStubbedNamedElementImpl<ElmTypeDeclarationStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmTypeDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    /**
     * Zero-or-more parametric type variables which may appear in the union members.
     */
    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerTypeName::class.java)

    /**
     * The union members which define the structure of the type.
     *
     * In a well-formed program, this will contain at least one element.
     */
    val unionMemberList: List<ElmUnionMember>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmUnionMember::class.java)

}
