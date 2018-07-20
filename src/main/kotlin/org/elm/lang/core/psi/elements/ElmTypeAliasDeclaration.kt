package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.icons.ElmIcons
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmTypeAliasDeclarationStub


/**
 * Declares a type alias
 *
 * e.g. `type alias User = { name : String, age : Int }`
 *
 */
class ElmTypeAliasDeclaration : ElmStubbedNamedElementImpl<ElmTypeAliasDeclarationStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.UPPER)

    constructor(stub: ElmTypeAliasDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.UPPER)


    override fun getIcon(flags: Int) =
            ElmIcons.TYPE_ALIAS


    /** The new name (alias) which will hereafter refer to [typeRef] */
    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /** Zero-or-more type variables which may appear in [typeRef] */
    val lowerTypeNameList: List<ElmLowerTypeName>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerTypeName::class.java)

    /**
     * The type which is being aliased
     *
     * In a well-formed program, this will be non-null.
     */
    val typeRef: ElmTypeRef?
        get() = findChildByClass(ElmTypeRef::class.java)


    /** `true` if the alias is exclusively a record */
    val isRecordAlias: Boolean
        get() = stub?.isRecordAlias ?: (typeRef?.firstChild is ElmRecordType)
}
