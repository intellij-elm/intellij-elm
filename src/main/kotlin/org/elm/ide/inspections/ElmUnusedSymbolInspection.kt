package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES
import com.intellij.psi.search.searches.ReferencesSearch
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * Find unused functions, parameters, etc.
 */
class ElmUnusedSymbolInspection : ElmLocalInspection() {

    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmNameIdentifierOwner) return
        val project = element.project
        val scope = GlobalSearchScope.projectScope(project)
        val name = element.name

        // ignore certain kinds of declarations which we don't want to inspect
        when (element) {
            is ElmTypeAliasDeclaration -> return  // TODO revisit: implementation is a little tricky so punting for now
            is ElmTypeDeclaration -> return       // TODO revisit: implementation is a little tricky so punting for now
            is ElmFunctionDeclarationLeft ->
                if (element.name == "main") return // assumed to be the program entry-point
        }

        // to keep inspection/analysis time brief, bail out if 'Find Usages' will be slow
        val searchCost = PsiSearchHelper.SERVICE.getInstance(project).isCheapEnoughToSearch(name, scope, null, null)
        if (searchCost == TOO_MANY_OCCURRENCES) return

        // perform Find Usages
        val usages = ReferencesSearch.search(element).findAll()
                .filterNot { it.element is ElmTypeAnnotation || it.element is ElmExposedItemTag }

        if (usages.isEmpty()) {
            markAsUnused(holder, element, name)
        }
    }

    private fun markAsUnused(holder: ProblemsHolder, element: ElmNameIdentifierOwner, name: String) {
        holder.registerProblem(
                element.nameIdentifier,
                "'$name' is never used",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }
}