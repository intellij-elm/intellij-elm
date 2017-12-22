package org.elm.ide.intentions

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.ElmTypes.START_DOC_COMMENT
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.stubs.index.ElmNamedElementIndex
import org.elm.openapiext.toPsiFile

data class Context(val fullRefName: String, val candidates: List<Candidate>) {
    val importAsQualified: Boolean
        get() = fullRefName.contains(".")
}

class ElmImportIntentionAction: ElmAtCaretIntentionActionBase<Context>() {

    override fun getText() = "Import"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        if (element.parentOfType<ElmImportClause>() != null) return null
        val refElement = element.parentOfType<ElmReferenceElement>() ?: return null

        val name = refElement.referenceName
        val scope = GlobalSearchScope.allScope(project)
        val candidates = ElmNamedElementIndex.find(name, project, scope)
                .mapNotNull { Candidate.fromNamedElement(it) }

        if (candidates.isEmpty())
            return null

        return Context(refElement.text, candidates)
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
        val newImport = if (context.importAsQualified)
            factory.createImport(candidate.moduleName)
        else
            factory.createImportExposing(candidate.moduleName, listOf(candidate.nameForImport))

        // TODO [kl] find existing import
        val existingImport: ElmImportClause? = null
        if (existingImport != null) {
            // merge with existing import
            // TODO [kl]
        } else {
            // insert a new import clause
            val insertPosition = getInsertPosition(file, candidate.moduleName)
            doInsert(newImport, insertPosition)
        }
    }

    private fun getInsertPosition(file: ElmFile, moduleName: String): ASTNode {
        val existingImportClauses = ModuleScope(file).getImportDecls()
        return if (existingImportClauses.isEmpty())
            prepareInsertInNewSection(file)
        else
            getSortedInsertPosition(moduleName, existingImportClauses.toList())
    }

    private fun prepareInsertInNewSection(sourceFile: ElmFile): ASTNode {
        val project = sourceFile.project
        val moduleDecl = sourceFile.getModuleDecl()

        if (moduleDecl == null) {
            // source file does not have an explicit module declaration
            // so just insert at the front of the file.
            return sourceFile.node.firstChildNode
        } else {
            // it does have a module decl, so find the right place to insert
            // the import after the module decl

            // import clauses must come *after* module documentation comments
            val importSectionAnchor = skipOverDocComments(moduleDecl)

            // insert blanklines flanking the new import section
            val newFreshline = ElmPsiFactory(project).createFreshLine().getNode()
            sourceFile.node.addChild(newFreshline, importSectionAnchor!!.node)
            val newFreshline2 = ElmPsiFactory(project).createFreshLine().getNode()
            sourceFile.node.addChild(newFreshline2, newFreshline)
            return newFreshline.treeNext
        }
    }

    private fun skipOverDocComments(startElement: PsiElement): PsiElement? {
        val elt = startElement.nextSibling
                ?: return startElement

        if (elt is PsiComment)
            if (elt.tokenType === START_DOC_COMMENT)
                return PsiTreeUtil.skipSiblingsForward(elt, PsiComment::class.java)

        return elt
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

        val firstImport = existingImportClauses[0]
        val lastImport = existingImportClauses[existingImportClauses.size - 1]

        return when {
            compareImportAndModule(firstImport, moduleName) >= 0 ->
                firstImport.node

            compareImportAndModule(lastImport, moduleName) < 0 ->
                lastImport.node.treeNext

            else ->
                // find the correct position somewhere in the middle
                // TODO [kl] simplify this ordering logic (code was ported from Java 8)
                existingImportClauses
                        .zip(existingImportClauses.subList(1, existingImportClauses.size))
                        .filter({ pair -> compareImportAndModule(pair.first, moduleName) < 0
                                       && compareImportAndModule(pair.second, moduleName) >= 0 })
                        .map({ pair -> pair.second.node })
                        .firstOrNull()
                        ?: error("should not happen: import not found in the middle")
        }
    }

    private fun doInsert(importClause: ElmImportClause, insertPosition: ASTNode) {
        val project = importClause.project
        val parent = insertPosition.treeParent
        val beforeInsertPosition = insertPosition.treePrev

        // ensure that a freshline exists immediately following
        // where we are going to insert the new import clause.
        var prevFreshline: ASTNode? = null
        if (insertPosition.elementType !== ElmTypes.NEWLINE) {
            prevFreshline = ElmPsiFactory(project).createFreshLine().getNode()
            parent.addChild(prevFreshline!!, insertPosition)
        } else {
            prevFreshline = insertPosition
        }

        // insert the import clause before the freshline
        parent.addChild(importClause.node, prevFreshline)

        // ensure that freshline exists *before* the new import clause
        if (beforeInsertPosition != null && beforeInsertPosition.elementType !== ElmTypes.NEWLINE) {
            val newFreshline = ElmPsiFactory(project).createFreshLine().getNode()
            parent.addChild(newFreshline, importClause.node)
        }
    }

    private fun promptToSelectCandidate(context: Context, file: ElmFile) {
        // TODO implement me
        throw UnsupportedOperationException("not implemented")
    }
}


/**
 * @param moduleName    the module where this value/type lives
 * @param name          the name of the value/type
 * @param nameForImport the name suitable for insert into an exposing clause.
 *                      Typically this is the same as `name`, but when importing
 *                      a bare union type member, it will be the parenthesized
 *                      form: "TypeName(MemberName)"
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
            return if (moduleDecl.exposesAll || exposingList.exposesName(name))
                Candidate(
                        moduleName = moduleDecl.name,
                        name = name,
                        nameForImport = name,
                        targetElement = element)
            else
                null
        }
    }
}
