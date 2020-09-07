package org.elm.ide.inspections

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmValueExpr

class MakeDeclarationFromUsageFix : NamedQuickFix("Create") {
    override fun applyFix(element: PsiElement, project: Project) {
        if (element !is ElmValueExpr) {
           return
        }

        // addTopLevelFunction("greet", psiFactory)
        val psiFactory = ElmPsiFactory(project)
        val s = element.referenceName
        val valueDeclaration = psiFactory.createTopLevelFunction("$s = Debug.todo \"TODO\"")
        // spliceIntoTopLevel
        element.elmFile.add(valueDeclaration)
    }


}
