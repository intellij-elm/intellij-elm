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
import org.elm.lang.core.imports.ImportAdder
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.moduleName

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
            .mapNotNull {
                when (it) {
                    is ElmValueDeclaration -> it.functionDeclarationLeft
                    else -> it
                }
            }
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
            val itemNames: List<String> = itemsToMove
                .mapNotNull {
                    when (it) {
                        is ElmValueDeclaration -> it.functionDeclarationLeft?.lowerCaseIdentifier?.text
                        is ElmTypeAliasDeclaration -> it.name
                        is ElmTypeDeclaration -> it.name + "(..)"
                        else -> null
                    }
                }

            itemNames.forEach(exposingList::addItem)

            usages
                .mapNotNull { it.file as ElmFile? }
                .distinct()
                .forEach {
                    var index: Int = getImportClauseIndex(it, moduleDeclaration)
                    var fileAdded = false
                    var names: List<String> = itemNames

                    if (index == -1 && itemNames.isNotEmpty()) {
                        ImportAdder.addImport(
                            ImportAdder.Import(moduleDeclaration.moduleName, null, itemNames.first()),
                            it,
                            false
                        )
                        fileAdded = true
                        index = getImportClauseIndex(it, moduleDeclaration)
                    }

                    if (!it.getImportClauses()[index].exposesAll) {
                        if (fileAdded) {
                            names = names.drop(1)
                        }

                        val importingList: ElmExposingList? = it.getImportClauses()[index].exposingList
                        names.forEach { importingList?.addItem(it) }
                    }
                }
        }
    }

    private fun getImportClauseIndex(
        file: ElmFile,
        moduleDeclaration: ElmModuleDeclaration
    ) = file
        .getImportClauses()
        .map(ElmImportClause::referenceName)
        .indexOf(moduleDeclaration.moduleName)

    override fun getCommandName(): String {
        return "Move Items"
    }

}
