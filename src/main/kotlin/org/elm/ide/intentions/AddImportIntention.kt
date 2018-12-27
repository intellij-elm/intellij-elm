package org.elm.ide.intentions

import com.intellij.lang.ASTNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBList
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.stubs.index.ElmNamedElementIndex
import org.elm.openapiext.toPsiFile
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class AddImportIntention : ElmAtCaretIntentionActionBase<AddImportIntention.Context>() {

    data class Context(
            val refName: String,
            val candidates: List<Candidate>,
            val isQualified: Boolean
    )

    override fun getText() = "Import"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        if (element.parentOfType<ElmImportClause>() != null) return null
        val refElement = element.parentOfType<ElmReferenceElement>() ?: return null

        // we can't import the function we're annotating
        if (refElement is ElmTypeAnnotation) return null

        val fullName = refElement.text      // e.g. `Html.div`
        val name = refElement.referenceName // e.g. `div`
        val scope = GlobalSearchScope.allScope(project)
        val candidates = ElmNamedElementIndex.find(name, project, scope)
                .mapNotNull { Candidate.fromNamedElement(it) }
                .toMutableList()

        val isQualified: Boolean
        if (fullName.contains(".") && refElement !is ElmOperator) {
            isQualified = true
            // exclude any modules that don't match the qualifier prefix
            val qualifierPrefix = fullName.split(".").dropLast(1).joinToString(".")
            candidates.removeIf { it.moduleName != qualifierPrefix }
        } else {
            isQualified = false
        }

        // De-dupe multiple results with the same module-name. Normally you would never
        // have multiple modules with the same name, but until we start parsing the
        // source roots out of the `elm-package.json` file, we will have to do this
        // workaround.
        val dedupedCandidates = candidates.associateBy { it.moduleName }.values

        if (dedupedCandidates.isEmpty())
            return null

        return Context(name, dedupedCandidates.toList(), isQualified)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val file = editor.toPsiFile(project) as? ElmFile ?: error("no file: should not happen")
        when (context.candidates.size) {
            0 -> error("should not happen: must be at least one candidate")
            1 -> addImportForCandidate(context.candidates.first(), file, context)
            else -> promptToSelectCandidate(context, file)
        }
    }

    private fun addImportForCandidate(candidate: Candidate, file: ElmFile, context: Context) {
        val factory = ElmPsiFactory(file.project)
        val newImport = if (context.isQualified)
            factory.createImport(candidate.moduleName)
        else
            factory.createImportExposing(candidate.moduleName, listOf(candidate.nameForImport))

        val existingImport = ModuleScope(file).getImportDecls()
                .find { it.moduleQID.text == candidate.moduleName }
        if (existingImport != null) {
            // merge with existing import
            val mergedImport = mergeImports(file, existingImport, newImport)
            existingImport.replace(mergedImport)
        } else {
            // insert a new import clause
            val insertPosition = getInsertPosition(file, candidate.moduleName)
            doInsert(newImport, insertPosition)
        }
    }

    private fun doInsert(importClause: ElmImportClause, insertPosition: ASTNode) {
        val parent = insertPosition.treeParent
        val factory = ElmPsiFactory(importClause.project)
        // insert the import clause followed by a newline immediately before `insertPosition`
        val newlineNode = factory.createFreshLine().node
        parent.addChild(newlineNode, insertPosition)
        parent.addChild(importClause.node, newlineNode)
    }

    /**
     * Returns the node which will *follow* the new import clause
     */
    private fun getInsertPosition(file: ElmFile, moduleName: String): ASTNode {
        val existingImportClauses = ModuleScope(file).getImportDecls()
        return if (existingImportClauses.isEmpty())
            prepareInsertInNewSection(file)
        else
            getSortedInsertPosition(moduleName, existingImportClauses.toList())
    }

    private val topLevelDeclarationTypes = tokenSetOf(
            TYPE_DECLARATION, TYPE_ALIAS_DECLARATION, VALUE_DECLARATION,
            TYPE_ANNOTATION, PORT_ANNOTATION
    )

    private fun prepareInsertInNewSection(sourceFile: ElmFile): ASTNode {
        // prepare for insert immediately before the first top-level declaration
        return sourceFile.node.findChildByType(topLevelDeclarationTypes)!!
    }

    private fun compareImportAndModule(importClause: ElmImportClause, moduleName: String): Int {
        return importClause.moduleQID.text.compareTo(moduleName)
    }

    private fun getSortedInsertPosition(moduleName: String, existingImportClauses: List<ElmImportClause>): ASTNode {
        // NOTE: we *assume* that the imports are already sorted and we
        // do not make any distinction between import groups/sections
        // (e.g. the practice of putting core libs in the first group,
        // 3rd party libs in a second group, and project files in the
        // final group).

        val firstImport = existingImportClauses.first()
        val lastImport = existingImportClauses.last()

        return when {
            compareImportAndModule(firstImport, moduleName) >= 0 ->
                firstImport.node

            compareImportAndModule(lastImport, moduleName) < 0 -> {
                // go past the last import and its newline
                var node = lastImport.node.treeNext
                while (!node.textContains('\n')) {
                    node = node.treeNext
                }
                return node.treeNext
            }

            else ->
                // find the correct position somewhere in the middle
                // TODO [kl] simplify this ordering logic (code was ported from Java 8)
                existingImportClauses
                        .zip(existingImportClauses.subList(1, existingImportClauses.size))
                        .filter({ pair ->
                            compareImportAndModule(pair.first, moduleName) < 0
                                    && compareImportAndModule(pair.second, moduleName) >= 0
                        })
                        .map({ pair -> pair.second.node })
                        .firstOrNull()
                        ?: error("should not happen: import not found in the middle")
        }
    }

    private fun promptToSelectCandidate(context: Context, file: ElmFile) {
        require(context.candidates.isNotEmpty())
        /* TODO [kl] this code was ported from Java. Check the Rust plugin
           to see if they have a nicer way to build these picker UIs. */
        val project = file.project
        val list = JBList(context.candidates.sortedBy { it.moduleName })
        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int,
                                                      isSelected: Boolean, cellHasFocus: Boolean): Component {
                val result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = (value as? Candidate)?.moduleName
                return result
            }
        }
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        JBPopupFactory.getInstance().createListPopupBuilder(list)
                .setTitle("Import '${context.refName}' from module:")
                .setItemChoosenCallback {
                    val value = list.getSelectedValue()
                    if (value is Candidate) {
                        runWriteActionToImportCandidate(value, file, context)
                    }
                }
                .setFilteringEnabled { value -> (value as Candidate).moduleName }
                .createPopup().showInBestPositionFor(editor!!)
    }

    private fun runWriteActionToImportCandidate(candidate: Candidate, file: ElmFile, context: Context) {
        object : WriteCommandAction.Simple<Unit>(file.project) {
            override fun run() {
                addImportForCandidate(candidate, file, context)
            }
        }.execute()
    }
}


/**
 * @param moduleName    the module where this value/type lives
 * @param name          the name of the value/type
 * @param nameForImport the name suitable for insert into an exposing clause.
 *                      Typically this is the same as `name`, but when importing
 *                      a bare union type variant, it will be the parenthesized
 *                      form: "TypeName(VariantName)"
 * @param targetElement the value/type element in the module-to-be-imported
 */
data class Candidate(
        val moduleName: String,
        val name: String,
        val nameForImport: String,
        val targetElement: ElmNamedElement) {

    companion object {

        /**
         * Returns a candidate if the named element is exposed by its containing module
         */
        fun fromNamedElement(element: ElmNamedElement): Candidate? {
            val moduleDecl = element.elmFile.getModuleDecl() ?: return null
            val exposingList = moduleDecl.exposingList ?: return null
            val name = element.name!!

            val (nameForImport, isExposedDirectly) = when (element) {
                is ElmUnionVariant -> {
                    val typeName = element.parentOfType<ElmTypeDeclaration>()!!.name
                    Pair("$typeName(..)", exposingList.exposesConstructor(name, typeName))
                }

                is ElmInfixDeclaration ->
                    Pair("($name)", exposingList.exposesName(name))

                // TODO [drop 0.18] remove this clause
                is ElmOperatorDeclarationLeft ->
                    Pair("($name)", exposingList.exposesName(name))

                else ->
                    Pair(name, exposingList.exposesName(name))
            }

            return if (moduleDecl.exposesAll || isExposedDirectly)
                Candidate(
                        moduleName = moduleDecl.name,
                        name = name,
                        nameForImport = nameForImport,
                        targetElement = element)
            else
                null
        }
    }
}


/**
 * Returns true if [name] can be found in the list of exposed values, constructors and types
 *
 * NOTE: this doesn't handle the case where a union type exposes all of its variants.
 * For that case, see [ElmExposingList.exposesConstructor]
 *
 * TODO [kl] cleanup: there's got to be a cleaner way to do this. it also seems like it
 * should be able to share code with how resolving an import reference needs to make sure
 * that the target element is actually exposed by the module. See [ImportScope]
 */
private fun ElmExposingList.exposesName(name: String): Boolean =
        sequenceOf<List<ElmReferenceElement>>(
                exposedValueList,
                exposedTypeList,
                exposedOperatorList)
                .flatten()
                .map { it.referenceName }
                .contains(name)

private fun ElmExposingList.exposesConstructor(constructorName: String, typeName: String): Boolean =
        exposedTypeList.any {
            (it.referenceName == typeName && it.exposesAll)
                    || it.exposesConstructorExplicitly(constructorName)
        }

private fun ElmExposedType.exposesConstructorExplicitly(name: String): Boolean =
        exposedUnionConstructors?.exposedUnionConstructors
                ?.any { it.referenceName == name }
                ?: false
