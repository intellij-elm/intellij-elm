package org.elm.ide.code.format

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.elm.ide.inspections.ImportVisitor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.removeItem
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.psi.prevSiblings
import org.elm.lang.core.resolve.scope.ModuleScope

class ElmImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile?) = file is ElmFile

    override fun processFile(file: PsiFile?): Runnable {
        return Runnable {
            if(file !is ElmFile)
                return@Runnable

            val visitor = ImportVisitor(ModuleScope.getImportDecls(file))

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
}