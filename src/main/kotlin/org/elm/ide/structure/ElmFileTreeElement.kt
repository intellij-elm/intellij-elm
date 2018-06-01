package org.elm.ide.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import org.elm.lang.core.psi.ElmFile


class ElmFileTreeElement(element: ElmFile) : PsiTreeElementBase<ElmFile>(element) {

    override fun getPresentableText(): String? {
        return element?.name
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val file = element ?: return emptyList()

        return listOf(
                file.getValueDeclarations(),
                file.getTypeAliasDeclarations(),
                file.getTypeDeclarations(),
                file.getPortAnnotations()
        )
                .flatten()
                .map { ElmPresentableTreeElement(it) }
                .sortedBy { it.element.textOffset }
    }
}