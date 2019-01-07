package org.elm.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.elm.ide.inspections.ImportVisitor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.removeItem
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.psi.prevSiblings
import org.elm.lang.core.resolve.scope.ModuleScope

class OptimizeImportsFix : LocalQuickFix {

    override fun getName() = "Optimize imports"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement?.containingFile as? ElmFile ?: return
        val visitor = ImportVisitor(ModuleScope(file).getImportDecls())

        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                super.visitElement(element)
            }
        })

        for (unusedImport in visitor.unusedImports) {
            val prevNewline = unusedImport.prevSiblings.firstOrNull { it.textContains('\n') }
            if (prevNewline == null) unusedImport.delete()
            else unusedImport.parent.deleteChildRange(prevNewline, unusedImport)
        }

        for (item in visitor.unusedExposedItems) {
            val exposingList = item.parentOfType<ElmImportClause>()?.exposingList ?: continue
            if (exposingList.allExposedItems.size <= 1) exposingList.delete()
            else exposingList.removeItem(item)
        }
    }
}