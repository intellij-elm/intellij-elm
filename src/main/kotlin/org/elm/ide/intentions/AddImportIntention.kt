package org.elm.ide.intentions

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.QualifiedReference
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.openapiext.isUnitTestMode
import org.elm.openapiext.runWriteCommandAction
import org.elm.openapiext.toPsiFile
import org.jetbrains.annotations.TestOnly
import javax.swing.JList


interface ImportPickerUI {
    fun choose(candidates: List<Candidate>, callback: (Candidate) -> Unit)
}

private var MOCK: ImportPickerUI? = null

@TestOnly
fun withMockUI(mockUi: ImportPickerUI, action: () -> Unit) {
    MOCK = mockUi
    try {
        action()
    } finally {
        MOCK = null
    }
}


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
        val ref = refElement.reference

        // we can't import the function we're annotating
        if (refElement is ElmTypeAnnotation) return null

        val name = refElement.referenceName
        val candidates = ElmLookup.findByName<ElmExposableTag>(name, refElement.elmFile)
                .mapNotNull { Candidate.fromExposableElement(it, ref) }
                .toMutableList()

        if (candidates.isEmpty())
            return null

        return Context(name, candidates, ref is QualifiedReference)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val file = editor.toPsiFile(project) as? ElmFile ?: error("no file: should not happen")
        when (context.candidates.size) {
            0 -> error("should not happen: must be at least one candidate")
            1 -> {
                val candidate = context.candidates.first()
                // Normally we would just directly perform the import here without prompting,
                // but if it's an alias-based import, we should show the user some UI so that
                // they know what they're getting into. See https://github.com/klazuka/intellij-elm/issues/309
                when {
                    candidate.moduleAlias != null -> promptToSelectCandidate(context, file)
                    else -> addImportForCandidate(candidate, file, context)
                }
            }
            else -> promptToSelectCandidate(context, file)
        }
    }

    private fun addImportForCandidate(candidate: Candidate, file: ElmFile, context: Context) {
        val factory = ElmPsiFactory(file.project)
        val newImport = if (context.isQualified)
            factory.createImport(candidate.moduleName, alias = candidate.moduleAlias)
        else
            factory.createImportExposing(candidate.moduleName, listOf(candidate.nameToBeExposed))

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
        val existingImports = ModuleScope(file).getImportDecls()
        return when {
            existingImports.isEmpty() -> prepareInsertInNewSection(file)
            else -> getSortedInsertPosition(moduleName, existingImports)
        }
    }

    private fun prepareInsertInNewSection(sourceFile: ElmFile): ASTNode {
        // prepare for insert immediately before the first top-level declaration
        return sourceFile.node.findChildByType(ELM_TOP_LEVEL_DECLARATIONS)!!
    }

    private fun getSortedInsertPosition(moduleName: String, existingImports: List<ElmImportClause>): ASTNode {
        // NOTE: assumes that they are already sorted
        for (import in existingImports) {
            if (moduleName < import.moduleQID.text)
                return import.node
        }

        // It belongs at the end: go past the last import and its newline
        var node = existingImports.last().node.treeNext
        while (!node.textContains('\n')) {
            node = node.treeNext
        }
        return node.treeNext
    }

    private fun promptToSelectCandidate(context: Context, file: ElmFile) {
        require(context.candidates.isNotEmpty())
        val candidates = context.candidates.sortedBy { it.moduleName }
        val project = file.project

        val picker = if (isUnitTestMode) {
            MOCK ?: error("You must set mock UI via `withMockUI`")
        } else {
            PickerUI(project, context)
        }
        picker.choose(candidates) { candidate ->
            project.runWriteCommandAction {
                addImportForCandidate(candidate, file, context)
            }
        }
    }
}

class PickerUI(val project: Project, val context: AddImportIntention.Context) : ImportPickerUI {
    override fun choose(candidates: List<Candidate>, callback: (Candidate) -> Unit) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        JBPopupFactory.getInstance().createPopupChooserBuilder(candidates)
                .setTitle("Import '${context.refName}' from module:")
                .setItemChosenCallback { callback(it) }
                .setNamerForFiltering { it.moduleName }
                .setRenderer(CandidateRenderer())
                .createPopup().showInBestPositionFor(editor)
    }
}

private class CandidateRenderer : ColoredListCellRenderer<Candidate>() {
    override fun customizeCellRenderer(list: JList<out Candidate>, value: Candidate, index: Int, selected: Boolean, hasFocus: Boolean) {
        // TODO set the background color based on project vs tests vs library
        append(value.moduleName)
        if (value.moduleAlias != null) {
            val attr = when {
                selected -> SimpleTextAttributes.REGULAR_ATTRIBUTES
                else -> SimpleTextAttributes.GRAYED_ATTRIBUTES
            }
            append(" as ${value.moduleAlias}", attr)
        }
    }
}


/**
 * @param moduleName    the module where this value/type lives
 * @param moduleAlias   if present, the alias to use when importing [moduleName]
 * @param name          the name of the value/type
 * @param nameToBeExposed the name suitable for insert into an exposing clause.
 *                      Typically this is the same as `name`, but when importing
 *                      a bare union type variant, it will be the parenthesized
 *                      form: "TypeName(VariantName)"
 * @param targetElement the value/type element in the module-to-be-imported
 */
data class Candidate(
        val moduleName: String,
        val moduleAlias: String?,
        val name: String,
        val nameToBeExposed: String,
        val targetElement: ElmNamedElement) {

    companion object {

        /**
         * Returns a candidate if the element is exposed by its containing module
         */
        fun fromExposableElement(element: ElmExposableTag, ref: ElmReference): Candidate? {
            val moduleDecl = element.elmFile.getModuleDecl() ?: return null
            val exposingList = moduleDecl.exposingList ?: return null

            if (!exposingList.exposes(element))
                return null

            val nameToBeExposed = when (element) {
                is ElmUnionVariant -> {
                    val typeName = element.parentOfType<ElmTypeDeclaration>()!!.name
                    "$typeName(..)"
                }

                is ElmInfixDeclaration ->
                    "(${element.name})"

                // TODO [drop 0.18] remove this clause
                is ElmOperatorDeclarationLeft ->
                    "(${element.name})"

                else ->
                    element.name
            }

            val alias = inferModuleAlias(ref, moduleDecl)
            if (alias != null && alias.contains('.')) {
                // invalid candidate because the alias would violate Elm syntax
                return null
            }

            return Candidate(
                    moduleName = moduleDecl.name,
                    moduleAlias = alias,
                    name = element.name,
                    nameToBeExposed = nameToBeExposed,
                    targetElement = element)
        }

        /**
         * Attempt to infer an alias to be used when importing this module.
         */
        private fun inferModuleAlias(ref: ElmReference, moduleDecl: ElmModuleDeclaration): String? =
                if (ref is QualifiedReference && ref.qualifierPrefix != moduleDecl.name)
                    ref.qualifierPrefix
                else
                    null
    }
}