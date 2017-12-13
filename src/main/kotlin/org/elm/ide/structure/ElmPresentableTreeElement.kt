package org.elm.ide.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.pom.Navigatable
import com.intellij.psi.NavigatablePsiElement
import org.elm.ide.presentation.getPresentationForStructure
import org.elm.lang.core.psi.ElmPsiElement


class ElmPresentableTreeElement(val element: ElmPsiElement)
    : StructureViewTreeElement,
      Navigatable by (element as NavigatablePsiElement) {


    override fun getChildren(): Array<TreeElement> =
            emptyArray()

    override fun getValue() =
            element

    override fun getPresentation() =
            getPresentationForStructure(element)

}