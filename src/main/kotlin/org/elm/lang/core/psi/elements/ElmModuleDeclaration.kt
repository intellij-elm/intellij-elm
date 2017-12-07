
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.stubs.ElmModuleDeclarationStub
import org.elm.lang.core.stubs.ElmNamedStub


/**
 * The module declaration at the top of a file.
 *
 * e.g. `module Data.User exposing (User, encode, decoder)`
 *
 * Role:
 * - give the module a name
 * - expose values and types
 */
class ElmModuleDeclaration : ElmStubbedElement<ElmModuleDeclarationStub>, ElmNamedElement {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmModuleDeclarationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)

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


    override fun getName(): String {
        val stub = getStub() as? ElmNamedStub
        if (stub != null) {
            // TODO [kl] how will the stub thing affect my Cmd and Sub hacks?
            return stub.name
        }

        val fullName = upperCaseQID.text
        // EVIL HACK to support the implicit, aliased import of Platform.Cmd and Platform.Sub
        // modules from Elm Core.
        // TODO [kl] re-visit this when I don't have a migraine
        return when (fullName) {
            "Platform.Cmd" -> "Cmd"
            "Platform.Sub" -> "Sub"
            else -> fullName
        }
    }

    override fun setName(name: String): PsiElement {
        val newQID = ElmPsiFactory(project).createUpperCaseQID(name)
        upperCaseQID.replace(newQID)
        return this
    }

    override fun getTextOffset() =
            upperCaseQID.textOffset
}
