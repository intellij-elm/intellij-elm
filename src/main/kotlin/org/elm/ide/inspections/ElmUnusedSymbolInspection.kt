package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES
import com.intellij.psi.search.searches.ReferencesSearch
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

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
        if (element is ElmTypeAliasDeclaration || element is ElmTypeDeclaration) {
            // TODO revisit: implementation for types is a little tricky since
            //      type annotations are optional; punting for now
            return
        }

        if (isProgramEntryPoint(element)) return

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

    private fun isProgramEntryPoint(element: ElmNameIdentifierOwner): Boolean =
            when {
                element is ElmFunctionDeclarationLeft ->
                    element.name == "main" || element.isElmTestEntryPoint()
                element is ElmPortAnnotation -> isExposed(element)
                else -> false
            }


    private fun markAsUnused(holder: ProblemsHolder, element: ElmNameIdentifierOwner, name: String) {
        holder.registerProblem(
                element.nameIdentifier,
                "'$name' is never used",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }
}


private fun ElmFunctionDeclarationLeft.isElmTestEntryPoint(): Boolean {
    val decl = parentOfType<ElmValueDeclaration>() ?: return false
    if (!decl.isTopLevel) return false
    val typeAnnotation = decl.typeAnnotation ?: return false

    // HACK: string suffix match is very naive, but it's cheap and easy to test.
    // The right thing to do would be to verify that the type resolves to
    // a type declared in the `elm-test` package. But setting that up for
    // `ElmUnusedSymbolInspectionTest` is a pain.
    // TODO revisit this later
    if (!typeAnnotation.text.endsWith(" : Test") && !typeAnnotation.text.endsWith(" : Test.Test"))
        return false

    // The elm-test runner requires that the entry-point be exposed by the module
    return isExposed(this)
}


private fun isExposed(decl: ElmExposableTag): Boolean {
    val exposingList = decl.elmFile.getModuleDecl()?.exposingList ?: return false
    return exposingList.exposes(decl)
}