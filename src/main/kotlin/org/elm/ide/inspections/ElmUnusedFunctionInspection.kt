package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES
import com.intellij.psi.search.searches.ReferencesSearch
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft

class ElmUnusedFunctionInspection : ElmLocalInspection() {

    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmFunctionDeclarationLeft) return
        val project = element.project
        val scope = GlobalSearchScope.projectScope(project)
        val name = element.name

        val searchCost = PsiSearchHelper.SERVICE.getInstance(project).isCheapEnoughToSearch(name, scope, null, null)

        if (searchCost != TOO_MANY_OCCURRENCES) {
            // TODO filter lame refs like type annotations and `exposing`
            val numOccurrences = ReferencesSearch.search(element).findAll().count()
            if (numOccurrences == 0) {
                holder.registerProblem(
                        element.nameIdentifier,
                        "Function '$name' is never used",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }
        }
    }
}