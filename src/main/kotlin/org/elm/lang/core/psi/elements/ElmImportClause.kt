package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.stubDirectChildrenOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.stubs.ElmPlaceholderStub
import org.elm.lang.core.stubs.index.ElmModulesIndex

/**
 * An import declaration at the top of the module.
 *
 * e.g. 'import Data.User exposing (User, name, age)'
 *
 * Role:
 * - refers to the module from which values and types should be imported
 * - possibly introduces an alias name for the module
 * - expose individual values and types from the module
 */
class ElmImportClause : ElmStubbedElement<ElmPlaceholderStub>, ElmReferenceElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmPlaceholderStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val moduleQID: ElmUpperCaseQID
        get() = stubDirectChildrenOfType<ElmUpperCaseQID>().single()

    val asClause: ElmAsClause?
        get() = stubDirectChildrenOfType<ElmAsClause>().singleOrNull()

    val exposingList: ElmExposingList?
        get() = stubDirectChildrenOfType<ElmExposingList>().singleOrNull()


    val exposesAll: Boolean
        get() = exposingList?.exposesAll ?: false


    override val referenceNameElement: PsiElement
        get() = moduleQID

    override val referenceName: String
        get() = moduleQID.fullName // stub-safe

    override fun getReference() =
            object : ElmReferenceCached<ElmImportClause>(this) {

                override fun resolveInner(): ElmNamedElement? =
                        ElmModulesIndex.get(moduleQID.fullName, elmFile)

                override fun getVariants(): Array<ElmNamedElement> =
                        ElmModulesIndex.getAll(elmFile).toTypedArray()
            }
}
