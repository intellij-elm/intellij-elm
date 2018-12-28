package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Find unused imports
 */
class ElmUnusedImportInspection : LocalInspectionTool() {

    private val visitorKey = Key.create<ImportVisitor>("")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        val file = session.file as? ElmFile
                ?: return super.buildVisitor(holder, isOnTheFly, session)
        val visitor = ImportVisitor(ModuleScope(file).getImportDecls())
        session.putUserData(visitorKey, visitor)
        return visitor
    }

    override fun inspectionFinished(session: LocalInspectionToolSession, problemsHolder: ProblemsHolder) {
        val visitor = session.getUserData(visitorKey) ?: return
        for (unusedImport in visitor.unusedImports) {
            markAsUnused(problemsHolder, unusedImport)
        }
    }

    private fun markAsUnused(holder: ProblemsHolder, importClause: ElmImportClause) {
        holder.registerProblem(
                importClause,
                "Unused import",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }

    private fun markAsUnused(holder: ProblemsHolder, exposedItem: ElmExposedItemTag) {
        holder.registerProblem(
                exposedItem,
                "'${exposedItem.text}' is unused",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }
}

class ImportVisitor(initialImports: List<ElmImportClause>) : PsiElementVisitor() {

    // IMPORTANT! IntelliJ's LocalInspectionTool requires that visitor implementations be thread-safe.
    private val imports = initialImports.toMutableList()

    /** Returns the list of unused imports. IMPORTANT: only valid *after* the visitor completes its traversal. */
    val unusedImports: List<ElmImportClause>
        get() = synchronized(this) { ArrayList(imports) }

    override fun visitElement(element: PsiElement?) {
        super.visitElement(element)
        if (element is ElmReferenceElement && element !is ElmImportClause && element !is ElmExposedItemTag) {
            // TODO possible performance optimization:
            //      Qualified refs may not need to be resolved as they have enough information to determine
            //      the target module name directly. But the refs may be cached, so...shrug
            val resolved = element.reference.resolve() ?: return
            val resolvedModule = resolved.elmFile.getModuleDecl() ?: return
            val resolvedModuleName = resolvedModule.name
            synchronized(this) {
                imports.removeIf { it.moduleQID.text == resolvedModuleName }
            }
        }
    }
}