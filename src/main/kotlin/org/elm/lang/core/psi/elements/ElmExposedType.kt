package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ExposedTypeReferenceFromImport
import org.elm.lang.core.resolve.reference.ExposedTypeReferenceFromModuleDecl
import org.elm.lang.core.stubs.ElmExposedTypeStub


/**
 * Exposes a named type. The type can be one of 2 things:
 * 1) a Union Type, in which case [exposedUnionConstructors] may be non-null
 * 2) a Type Alias, in which case [exposedUnionConstructors] will be null
 */
class ElmExposedType : ElmStubbedElement<ElmExposedTypeStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposedTypeStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)


    val exposedUnionConstructors: ElmExposedUnionConstructors?
        get() = PsiTreeUtil.getStubChildOfType(this, ElmExposedUnionConstructors::class.java)


    val exposesAll: Boolean
        get() = getStub()?.exposesAll
                ?: (exposedUnionConstructors?.doubleDot != null)


    override val referenceNameElement: PsiElement
        get() = upperCaseIdentifier

    override val referenceName: String
        get() = getStub()?.refName ?: referenceNameElement.text

    override fun getReference() =
            if (parentOfType<ElmModuleDeclaration>() != null) ExposedTypeReferenceFromModuleDecl(this)
            else if (parentOfType<ElmImportClause>() != null) ExposedTypeReferenceFromImport(this)
            else error("unexpected exposed type context: reference cannot be determined")
}


