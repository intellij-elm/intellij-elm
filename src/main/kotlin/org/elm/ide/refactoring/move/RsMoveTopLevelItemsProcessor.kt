package org.elm.ide.refactoring.move

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.*
import java.util.logging.Logger

class RsMoveTopLevelItemsProcessor(
    private val project: Project,
    private val itemsToMove: Array<out PsiElement>,
    private val targetMod: ElmFile,
    private val searchForReferences: Boolean
) : BaseRefactoringProcessor(project) {

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return MoveMultipleElementsViewDescriptor(emptyArray(), "")
    }

    override fun findUsages(): Array<UsageInfo> {
        if (!searchForReferences) return UsageInfo.EMPTY_ARRAY
        return itemsToMove
            .flatMap { ReferencesSearch.search(it, GlobalSearchScope.projectScope(project)) }
            .filterNotNull()
            .map { UsageInfo(it) }
            .toTypedArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val factory = ElmPsiFactory(project)
        val newline = factory.createNewline()

        targetMod.add(newline)

        for (item in itemsToMove) {
            val space = item.nextSibling as? PsiWhiteSpace

            targetMod.add(newline)
            // have to call `copy` because of rare suspicious `PsiInvalidElementAccessException`
            targetMod.add(item.copy())
            if (space != null) targetMod.add(space.copy())

            space?.delete()
            item.delete()
        }

        val moduleDeclaration: ElmModuleDeclaration? = targetMod.getModuleDecl()
        val exposingList: ElmExposingList? = moduleDeclaration?.exposingList

        if (usages.isNotEmpty() && moduleDeclaration != null && !moduleDeclaration.exposesAll && exposingList != null) {
            itemsToMove
                .mapNotNull {
                    when (it) {
                        is ElmValueDeclaration -> it.name
                        is ElmTypeAliasDeclaration -> it.name
                        is ElmTypeDeclaration -> it.name + "(..)"
                        else -> null
                    }
                }
                .forEach(exposingList::addItem)
        }
    }

    override fun getCommandName(): String {
        return "Move Items"
    }

}
