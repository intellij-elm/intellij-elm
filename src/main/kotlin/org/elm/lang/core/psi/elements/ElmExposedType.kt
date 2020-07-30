package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ExposedTypeReferenceFromImport
import org.elm.lang.core.resolve.reference.ExposedTypeReferenceFromModuleDecl
import org.elm.lang.core.stubs.ElmExposedTypeStub


/**
 * Exposes a named type. The type can be one of 2 things:
 * 1) a Union Type
 * 2) a Type Alias
 */
class ElmExposedType : ElmStubbedElement<ElmExposedTypeStub>, ElmReferenceElement, ElmExposedItemTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposedTypeStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)

    /**
     * Returns true for union types which expose their constructors/variants.
     */
    val exposesAll: Boolean
        get() = stub?.exposesAll ?: (findChildByType<PsiElement>(DOUBLE_DOT) != null)

    /**
     * Make an exposed union type opaque.
     */
    fun deleteParensAndDoubleDot() {
        val leftParen = directChildren.find { it.elementType == LEFT_PARENTHESIS }
        val rightParen = directChildren.find { it.elementType == RIGHT_PARENTHESIS }
        if (leftParen == null || rightParen == null)
            return
        deleteChildRange(leftParen, rightParen)
    }

    override val referenceNameElement: PsiElement
        get() = upperCaseIdentifier

    override val referenceName: String
        get() = stub?.refName ?: referenceNameElement.text

    override fun getReference() = when (parent?.parent) {
        is ElmModuleDeclaration -> ExposedTypeReferenceFromModuleDecl(this)
        is ElmImportClause -> ExposedTypeReferenceFromImport(this)
        else -> error("unexpected exposed type context: reference cannot be determined")
    }
}


fun ElmExposedType.exposes(variant: ElmUnionVariant): Boolean {
    val targetTypeDecl = reference.resolve()
    if (variant.parentOfType<ElmTypeDeclaration>() != targetTypeDecl) {
        // they do not belong to the same union type
        return false
    }

    return exposesAll
}




