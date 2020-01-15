package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmExposedUnionConstructors
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration

/**
 * Find statements like `import A exposing B(..)` where `B` is a type alias, making the statement invalid.
 */
class ElmExposingAliasVariantsInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement?) {
                super.visitElement(element)
                if (element !is ElmExposedType) return

                if (element.exposedUnionConstructors != null && element.reference.resolve() is ElmTypeAliasDeclaration) {
                    holder.registerProblem(element, "Invalid (..) on alias import", RemoveExposingListQuickFix(element))
                }
            }
        }
    }
}

private class RemoveExposingListQuickFix(element: ElmExposedType) : LocalQuickFixOnPsiElement(element) {
    override fun getText(): String = "Remove (..)"
    override fun getFamilyName(): String = text

    override fun isAvailable(): Boolean {
        return super.isAvailable() && getExposedVariants() != null
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        getExposedVariants()?.delete()
    }

    private fun getExposedVariants(): ElmExposedUnionConstructors? {
        return (startElement as? ElmExposedType)?.exposedUnionConstructors
    }
}
