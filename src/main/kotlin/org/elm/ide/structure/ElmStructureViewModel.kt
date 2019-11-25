package org.elm.ide.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmValueDeclaration

class ElmStructureViewModel(elmFile: ElmFile) :
        StructureViewModelBase(elmFile, ElmFileTreeElement(elmFile)),
        StructureViewModel.ElementInfoProvider {

    override fun getSorters() =
            arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?) = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean {
        // Only value declarations can have children. This is just an optimization, so return false
        // if we're not sure.
        return (element as? ElmPresentableTreeElement)?.element
                ?.let { it !is ElmValueDeclaration }
                ?: false
    }
}
