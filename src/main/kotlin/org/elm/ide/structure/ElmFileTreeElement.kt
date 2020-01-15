package org.elm.ide.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmPortAnnotation
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.stubDirectChildrenOfType


class ElmFileTreeElement(element: ElmFile) : PsiTreeElementBase<ElmFile>(element) {

    override fun getPresentableText(): String? {
        return element?.name
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val file = element ?: return emptyList()
        return file.stubDirectChildrenOfType<ElmPsiElement>()
                .filter {
                    it is ElmValueDeclaration ||
                            it is ElmTypeAliasDeclaration ||
                            it is ElmTypeDeclaration ||
                            it is ElmPortAnnotation
                }
                .map { ElmPresentableTreeElement(it) }
                .sortedBy { it.element.textOffset }
    }
}
