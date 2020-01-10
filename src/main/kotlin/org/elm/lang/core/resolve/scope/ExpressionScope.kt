package org.elm.lang.core.resolve.scope

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmAnonymousFunctionExpr
import org.elm.lang.core.psi.elements.ElmCaseOfBranch
import org.elm.lang.core.psi.elements.ElmLetInExpr
import org.elm.lang.core.psi.elements.ElmValueDeclaration


class ExpressionScope(val element: PsiElement) {

    fun getVisibleValues(): List<ElmNamedElement> {

        val results = mutableListOf<ElmNamedElement>()
        val declAncestors = mutableListOf<ElmValueDeclaration>()

        treeWalkUp(element) {
            when (it) {
                is ElmFile -> {
                    results.addAll(ModuleScope.getVisibleValues(it).all)
                    return@treeWalkUp false // stop
                }
                is ElmValueDeclaration -> {
                    declAncestors += it
                    results.addAll(it.declaredNames())
                }
                is ElmLetInExpr -> {
                    for (innerDecl in it.valueDeclarationList) {
                        val includeParameters = innerDecl in declAncestors
                        results.addAll(innerDecl.declaredNames(includeParameters))
                    }
                }
                is ElmCaseOfBranch -> results.addAll(it.destructuredNames)
                is ElmAnonymousFunctionExpr -> results.addAll(it.namedParameters)
            }

            return@treeWalkUp true // keep going
        }

        return results
    }
}

private fun treeWalkUp(start: PsiElement, callback: (PsiElement) -> Boolean) {
    var current: PsiElement? = start
    while (current != null) {
        val keepGoing = callback(current)
        if (!keepGoing) break
        current = current.parent
    }
}
