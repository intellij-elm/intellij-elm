package org.elm.ide.navigation

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.stubs.index.ElmNamedElementIndex


class ElmGoToSymbolContributor: ChooseByNameContributor {


    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<out String> {
        project ?: return emptyArray()
        return StubIndex.getInstance().getAllKeys(indexKey, project).toTypedArray()
    }


    override fun getItemsByName(name: String?,
                                pattern: String?,
                                project: Project?,
                                includeNonProjectItems: Boolean): Array<out NavigationItem> {

        if (project == null || name == null) {
            return emptyArray()
        }
        val scope = if (includeNonProjectItems)
            GlobalSearchScope.allScope(project)
        else
            GlobalSearchScope.projectScope(project)

        return StubIndex.getElements(indexKey, name, project, scope, ElmNamedElement::class.java)
                .toTypedArray<NavigationItem>()
    }

    companion object {
        private val indexKey = ElmNamedElementIndex.KEY
    }
}