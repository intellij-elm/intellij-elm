package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.stubs.index.ElmModulesIndex

private val log = logger<ElmImportClause>()

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
class ElmImportClause(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val moduleQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    val asClause: ElmAsClause?
        get() = findChildByClass(ElmAsClause::class.java)

    val exposingList: ElmExposingList?
        get() = findChildByClass(ElmExposingList::class.java)


    val exposesAll: Boolean
        get() = exposingList?.doubleDot != null


    override val referenceNameElement: PsiElement
        get() = moduleQID

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            object : ElmReferenceCached<ElmImportClause>(this) {

                override fun resolveInner(): ElmNamedElement? =
                        getVariants().find { it.name == element.referenceName }

                override fun getVariants(): Array<ElmNamedElement> =
                        ElmModulesIndex.getAll(project, element.elmProject).toTypedArray()
            }
}
