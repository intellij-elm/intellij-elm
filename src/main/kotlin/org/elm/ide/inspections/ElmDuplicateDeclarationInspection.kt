package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmTupleExpr
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Report errors when a file has multiple declarations with the same name.
 */
class ElmDuplicateDeclarationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement?) {
                super.visitElement(element)
                if (element !is ElmFile) return
                (ModuleScope.getDeclaredTypes(element) + ModuleScope.getDeclaredValues(element))
                        .array
                        .groupBy { it.name }
                        .values
                        .filter { it.size > 1 }
                        .flatten()
                        .forEach {
                            val target = if (it is ElmNameIdentifierOwner) it.nameIdentifier else it
                            holder.registerProblem(target, "Multiple declarations with name '${it.name}'")
                        }
            }
        }
    }
}
