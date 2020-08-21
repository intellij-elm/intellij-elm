package org.elm.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.isTopLevel

/**
 * Detects top-level value destructuring and marks it as an error.
 **/
class ElmTopLevelDestructuringInspection : ElmLocalInspection() {

    /*
    Prior to Elm 0.19, top-level destructuring was allowed, and so our parser was
    designed originally to accept it. I could change the parser, but that would make
    things complicated for nested declarations within a `let-in` expression. So we
    will instead mark the error with an inspection.
     */
    override fun visitElement(element: ElmPsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (element !is ElmValueDeclaration) return

        val pat = element.pattern
        if (pat != null && element.isTopLevel) {
            holder.registerProblem(pat, "Destructuring at the top-level is not allowed")
        }
    }
}
