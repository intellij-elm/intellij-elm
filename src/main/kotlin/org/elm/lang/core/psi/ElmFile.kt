package org.elm.lang.core.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.stubs.*


class ElmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElmLanguage) {

    override fun getFileType() =
            ElmFileType

    override fun toString() =
            "Elm File"

    override fun getIcon(flags: Int) =
            super.getIcon(flags)

    override fun getStub() =
            super.getStub() as ElmFileStub?

    fun isCore(): Boolean {
        val path = virtualFile?.path
                ?: return false

        return path.contains("/package/elm/core/")
                || path.contains("/packages/elm-lang/core/") // TODO [drop 0.18] delete the "packages" clause
    }

    override fun setName(name: String): PsiElement {
        val nameWithExtension = if (name.endsWith(".elm")) name else "$name.elm"
        return super.setName(nameWithExtension)
    }

    fun getModuleDecl() =
            getStubOrPsiChild(ElmModuleDeclarationStub.Type)

    fun getValueDeclarations() =
            getStubOrPsiChildren(ElmValueDeclarationStub.Type, emptyArray())

    fun getTypeDeclarations() =
            getStubOrPsiChildren(ElmTypeDeclarationStub.Type, emptyArray())

    fun getTypeAliasDeclarations() =
            getStubOrPsiChildren(ElmTypeAliasDeclarationStub.Type, emptyArray())

    fun getPortAnnotations() =
            getStubOrPsiChildren(ElmPortAnnotationStub.Type, emptyArray())

    fun getInfixDeclarations() =
            getStubOrPsiChildren(ElmInfixDeclarationStub.Type, emptyArray())


    // TODO [kl] I had to copy a bunch of stuff from StubBasedPsiElementBase to work with
    // children of this file in a stub-friendly way. I must be doing something strange.
    // What do other plugin developers do?

    /**
     * NOTE: copied from [StubBasedPsiElementBase] and ported to Kotlin
     *
     * @return a child of specified type, taken from stubs (if this element is currently stub-based) or AST (otherwise).
     */
    fun <Psi : PsiElement> getStubOrPsiChild(elementType: IStubElementType<out StubElement<*>, Psi>): Psi? {
        val stub = getGreenStub()
        if (stub != null) {
            val element = stub.findChildStubByType<Psi, StubElement<*>>(elementType)
            if (element != null) {
                return element.getPsi() as Psi?
            }
        } else {
            val childNode = node.findChildByType(elementType)
            if (childNode != null) {
                return childNode.psi as Psi
            }
        }
        return null
    }


    /**
     * NOTE: copied from [StubBasedPsiElementBase] and ported to Kotlin
     *
     * @return a not-null child of specified type, taken from stubs (if this element is currently stub-based) or AST (otherwise).
     */
    fun <S : StubElement<*>, Psi : PsiElement> getRequiredStubOrPsiChild(elementType: IStubElementType<S, Psi>): Psi {
        return getStubOrPsiChild(elementType)
                ?: error("Missing required child of type " + elementType + "; tree: " + DebugUtil.psiToString(this, false))
    }


    /**
     * NOTE: copied from [StubBasedPsiElementBase] and ported to Kotlin
     *
     * @return children of specified type, taken from stubs (if this element is currently stub-based) or AST (otherwise).
     */
    inline fun <S : StubElement<*>, reified Psi : PsiElement> getStubOrPsiChildren(elementType: IStubElementType<S, out Psi>, array: Array<Psi>): List<Psi> {
        val stub = greenStub
        if (stub != null) {
            return stub.getChildrenByType<Psi>(elementType, array).filterIsInstance<Psi>()
        } else {
            val nodes = SharedImplUtil.getChildrenOfType(node, elementType)
            val psiElements = nodes.map { it.getPsi(Psi::class.java) }
            return psiElements
        }
    }

}

val VirtualFile.isElmFile
    get() = fileType == ElmFileType
