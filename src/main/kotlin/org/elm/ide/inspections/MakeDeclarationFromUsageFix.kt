package org.elm.ide.inspections

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmValueExpr

class MakeDeclarationFromUsageFix : NamedQuickFix("Create") {
    override fun applyFix(element: PsiElement, project: Project) {
        if (element !is ElmValueExpr) {
           return
        }

        // addTopLevelFunction("greet", psiFactory)
        val psiFactory = ElmPsiFactory(project)
        val s = element.referenceName


        val arity = (element.parent as? ElmFunctionCallExpr)?.arguments?.count() ?: 0

        val hasArity0 = arity == 0
        val valueDeclaration =
       if (hasArity0) {
           psiFactory.createTopLevelFunction("$s = Debug.todo \"TODO\"")
       } else {
           psiFactory.createTopLevelFunction("$s a = Debug.todo \"TODO\"")
       }
        // spliceIntoTopLevel
        element.elmFile.add(valueDeclaration)
    }


}
