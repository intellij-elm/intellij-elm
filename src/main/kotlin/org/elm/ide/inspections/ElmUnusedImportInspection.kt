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
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.psi.elements.Flavor.BareValue
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import java.util.concurrent.ConcurrentHashMap

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

        for (unusedItem in visitor.unusedExposedItems) {
            markAsUnused(problemsHolder, unusedItem)
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
                "'${exposedItem.text}' is exposed but unused",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }
}

class ImportVisitor(initialImports: List<ElmImportClause>) : PsiElementVisitor() {

    // IMPORTANT! IntelliJ's LocalInspectionTool requires that visitor implementations be thread-safe.
    private val imports: ConcurrentHashMap<String, ElmImportClause>
    private val exposing: ConcurrentHashMap<String, ElmExposedValue>

    init {
        imports = ConcurrentHashMap(initialImports.associateBy { it.moduleQID.text })
        exposing = initialImports.mapNotNull { it.exposingList }
                .flatMap { it.exposedValueList }
                .associateBy { it.text }
                .let { ConcurrentHashMap(it) }
    }

    /** Returns the list of unused imports. IMPORTANT: only valid *after* the visitor completes its traversal. */
    val unusedImports: List<ElmImportClause>
        get() = imports.values.toList()

    /** Returns the list of unused exposed items. IMPORTANT: only valid *after* the visitor completes its traversal. */
    val unusedExposedItems: List<ElmExposedItemTag>
        get() = exposing.values.toList().filter {
            val import = it.parentOfType<ElmImportClause>() ?: return@filter false
            // don't bother reporting things where the entire import is unused
            import !in unusedImports
        }

    override fun visitElement(element: PsiElement?) {
        super.visitElement(element)
        if (element is ElmReferenceElement && element !is ElmImportClause && element !is ElmExposedItemTag) {
            // TODO possible performance optimization:
            //      Qualified refs may not need to be resolved as they have enough information to determine
            //      the target module name directly. But the refs may be cached, so...shrug
            val resolved = element.reference.resolve() ?: return
            val resolvedModule = resolved.elmFile.getModuleDecl() ?: return
            val resolvedModuleName = resolvedModule.name
            imports.remove(resolvedModuleName)

            // For now we are just going to mark exposed values/functions which are unused
            // TODO expand this to types, union variant constructors, and operators
            if (element is ElmValueExpr && element.flavor == BareValue) {
                exposing.remove(element.referenceName)
            }
        }
    }
}