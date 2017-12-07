package org.elm.lang.core.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmPsiElement


abstract class ElmStubElementType<StubT : StubElement<*>, PsiT : ElmPsiElement>(
        debugName: String
) : IStubElementType<StubT, PsiT>(debugName, ElmLanguage) {

    final override fun getExternalId() =
            "elm.${super.toString()}"

    protected fun createStubIfParentIsStub(node: ASTNode): Boolean {
        val parent = node.treeParent
        val parentType = parent.elementType
        return (parentType is IStubElementType<*, *> && parentType.shouldCreateStub(parent)) ||
                parentType is IStubFileElementType<*>
    }
}