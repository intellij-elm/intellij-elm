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

        treeWalkUp(element) {
            if (it is ElmFile) {
                results.addAll(ModuleScope.getVisibleValues(it).all)
                return@treeWalkUp false // stop
            }

            if (it is ElmNamedElement) {
                results.add(it)
            }

            if (it is ElmValueDeclaration) {
                results.addAll(it.declaredNames())
            }

            if (it is ElmLetInExpr) {
                for (innerDecl in it.valueDeclarationList) {
                    val includeParameters = element.ancestors.any { it === innerDecl }
                    results.addAll(innerDecl.declaredNames(includeParameters))
                }
            }

            if (it is ElmCaseOfBranch) {
                results.addAll(it.destructuredNames)
            }

            if (it is ElmAnonymousFunctionExpr) {
                results.addAll(it.namedParameters)
            }

            return@treeWalkUp true // keep going
        }

        return results
    }
}

fun treeWalkUp(start: PsiElement, callback: (PsiElement) -> Boolean) {
    var current: PsiElement? = start
    while (current != null) {
        val keepGoing = callback(current)
        if (!keepGoing) break
        current = current.parent
    }
}
