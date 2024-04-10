package org.elm.ide.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmPortAnnotation
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmValueDeclaration

class ElmStructureViewModel(editor: Editor?, elmFile: ElmFile) :
        StructureViewModelBase(elmFile, editor, ElmFileTreeElement(elmFile)),
        StructureViewModel.ElementInfoProvider {

    init {
        // This list is used by the scroll-to-source functionality: the nearest ancestor of the
        // element at the caret that is one of these classes will be scrolled to.
        withSuitableClasses(
                ElmValueDeclaration::class.java,
                ElmTypeAliasDeclaration::class.java,
                ElmTypeDeclaration::class.java,
                ElmPortAnnotation::class.java
        )
    }

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
