package org.elm.ide.navigation

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.elm.lang.core.stubs.index.ElmNamedElementIndex


class ElmGoToSymbolContributor : ChooseByNameContributor {


    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<out String> {
        project ?: return emptyArray()
        return ElmNamedElementIndex.getAllNames(project).toTypedArray()
    }


    override fun getItemsByName(name: String?,
                                pattern: String?,
                                project: Project?,
                                includeNonProjectItems: Boolean): Array<out NavigationItem> {
        if (project == null || name == null)
            return emptyArray()

        val scope = if (includeNonProjectItems)
            GlobalSearchScope.allScope(project)
        else
            GlobalSearchScope.projectScope(project)

        return ElmNamedElementIndex.find(name, project, scope)
                .toTypedArray<NavigationItem>()
    }
}