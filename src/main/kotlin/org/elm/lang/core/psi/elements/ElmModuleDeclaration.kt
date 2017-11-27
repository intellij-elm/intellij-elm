
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmPsiFactory


/**
 * The module declaration at the top of a file.
 *
 * e.g. `module Data.User exposing (User, encode, decoder)`
 *
 * Role:
 * - give the module a name
 * - expose values and types
 */
class ElmModuleDeclaration(node: ASTNode) : ElmPsiElementImpl(node), ElmNamedElement {

    /**
     * The fully-qualified name of the module
     */
    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    /**
     * The values and types exposed by this module
     */
    val exposingList: ElmExposingList
        get() = findNotNullChildByClass(ElmExposingList::class.java)

    /**
     * Very rare. This will only appear in Effect Manager modules.
     */
    val effectModuleDetailRecord: ElmRecord?
        get() = findChildByClass(ElmRecord::class.java)


    val exposesAll: Boolean
        get() = exposingList.doubleDot != null


    override fun getName() =
            upperCaseQID.text

    override fun setName(name: String): PsiElement {
        val newQID = ElmPsiFactory(project).createUpperCaseQID(name)
        upperCaseQID.replace(newQID)
        return this
    }

    override fun getTextOffset() =
            upperCaseQID.textOffset
}
