package org.elm.ide.intentions

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.ColoredListCellRenderer
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.QualifiedReference
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.openapiext.runWriteCommandAction
import org.elm.openapiext.toPsiFile
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
        val ref = refElement.reference

        // we can't import the function we're annotating
        if (refElement is ElmTypeAnnotation) return null

        val name = refElement.referenceName
        val candidates = ElmLookup.findByName<ElmExposableTag>(name, refElement.elmFile)
                .mapNotNull { Candidate.fromExposableElement(it) }
                .toMutableList()

        val isQualified: Boolean
        if (ref is QualifiedReference) {
            isQualified = true
            candidates.removeIf { it.moduleName != ref.qualifierPrefix }
        } else {
            isQualified = false
        }

        if (candidates.isEmpty())
            return null

        return Context(name, candidates.toList(), isQualified)
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
        val existingImports = ModuleScope(file).getImportDecls()
        return when {
            existingImports.isEmpty() -> prepareInsertInNewSection(file)
            else -> getSortedInsertPosition(moduleName, existingImports)
        }
    }

    private val topLevelDeclarationTypes = tokenSetOf(
            TYPE_DECLARATION, TYPE_ALIAS_DECLARATION, VALUE_DECLARATION,
            TYPE_ANNOTATION, PORT_ANNOTATION
    )

    private fun prepareInsertInNewSection(sourceFile: ElmFile): ASTNode {
        // prepare for insert immediately before the first top-level declaration
        return sourceFile.node.findChildByType(topLevelDeclarationTypes)!!
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
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        JBPopupFactory.getInstance().createPopupChooserBuilder(candidates)
                .setTitle("Import '${context.refName}' from module:")
                .setItemChosenCallback {
                    project.runWriteCommandAction { addImportForCandidate(it, file, context) }
                }
                .setNamerForFiltering { it.moduleName }
                .setRenderer(CandidateRenderer())
                .createPopup().showInBestPositionFor(editor)
    }
}


private class CandidateRenderer : ColoredListCellRenderer<Candidate>() {
    override fun customizeCellRenderer(list: JList<out Candidate>, value: Candidate, index: Int, selected: Boolean, hasFocus: Boolean) {
        // TODO set the background color based on project vs tests vs library
        append(value.moduleName)
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
         * Returns a candidate if the element is exposed by its containing module
         */
        fun fromExposableElement(element: ElmExposableTag): Candidate? {
            val moduleDecl = element.elmFile.getModuleDecl() ?: return null
            val exposingList = moduleDecl.exposingList ?: return null

            if (!exposingList.exposes(element))
                return null

            val nameForImport = when (element) {
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

            return Candidate(
                    moduleName = moduleDecl.name,
                    name = element.name,
                    nameForImport = nameForImport,
                    targetElement = element)
        }
    }
}