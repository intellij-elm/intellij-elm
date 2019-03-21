package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.stubs.ElmExposedOperatorStub


/**
 * Exposes a binary operator
 */
class ElmExposedOperator : ElmStubbedElement<ElmExposedOperatorStub>, ElmReferenceElement, ElmExposedItemTag {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposedOperatorStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val operatorIdentifier: PsiElement
        get() = findNotNullChildByType(OPERATOR_IDENTIFIER)

    val leftParen: PsiElement
        get() = findNotNullChildByType(ElmTypes.LEFT_PARENTHESIS)

    val rightParen: PsiElement
        get() = findNotNullChildByType(ElmTypes.RIGHT_PARENTHESIS)


    override val referenceNameElement: PsiElement
        get() = operatorIdentifier

    override val referenceName: String
        get() = getStub()?.refName ?: referenceNameElement.text

    override fun getReference() =
            if (parentOfType<ElmModuleDeclaration>() != null) ExposedOperatorModuleReference(this)
            else if (parentOfType<ElmImportClause>() != null) ExposedOperatorImportReference(this)
            else error("unexpected exposed operator context: reference cannot be determined")
}


/**
 * A binary operator reference from an 'exposing' list in a module declaration (points within the same file)
 */
class ExposedOperatorModuleReference(exposedValue: ElmExposedOperator
) : ElmReferenceCached<ElmExposedOperator>(exposedValue) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] verify: this was copied from ElmExposedValue's ref
        return ModuleScope.getDeclaredValues(element.elmFile).toTypedArray()
    }
}

/**
 * A binary operator reference from an `exposing` list in an import clause (points to a different file)
 */
class ExposedOperatorImportReference(exposedValue: ElmExposedOperator
) : ElmReferenceCached<ElmExposedOperator>(exposedValue) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] verify: this was copied from ElmExposedValue's ref
        val importClause = element.parentOfType<ElmImportClause>()
                ?: error("should never happen: this ref must be in an import")

        return ImportScope.fromImportDecl(importClause)
                ?.getExposedValues()?.toTypedArray()
                ?: emptyArray()
    }
}