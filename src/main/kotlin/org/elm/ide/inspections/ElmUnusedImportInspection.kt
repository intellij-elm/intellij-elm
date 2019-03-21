package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.elm.ide.inspections.fixes.OptimizeImportsFix
import org.elm.lang.core.psi.ElmExposedItemTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.LexicalValueReference
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
        val visitor = ImportVisitor(ModuleScope.getImportDecls(file))
        session.putUserData(visitorKey, visitor)
        return visitor
    }

    override fun inspectionFinished(session: LocalInspectionToolSession, problemsHolder: ProblemsHolder) {
        val visitor = session.getUserData(visitorKey) ?: return

        for (import in visitor.unusedImports) {
            problemsHolder.markUnused(import, "Unused import")
        }

        for (item in visitor.unusedExposedItems) {
            problemsHolder.markUnused(item, "'${item.text}' is exposed but unused")
        }
    }
}


private fun ProblemsHolder.markUnused(elem: PsiElement, message: String) {
    registerProblem(elem, message, ProblemHighlightType.LIKE_UNUSED_SYMBOL, OptimizeImportsFix())
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
        get() = imports.values.toList().filter { !it.safeToIgnore }

    /** Returns the list of unused exposed items. IMPORTANT: only valid *after* the visitor completes its traversal. */
    val unusedExposedItems: List<ElmExposedItemTag>
        get() = exposing.values.toList().filter {
            val import = it.parentOfType<ElmImportClause>() ?: return@filter false
            val alreadyReported = import in unusedImports
            !alreadyReported && !import.safeToIgnore
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
            if (element.reference is LexicalValueReference) {
                exposing.remove(element.referenceName)
            }
        }
    }
}


// Elm's Kernel modules are defined in JS, and our PsiReference system does not (currently)
// cross the boundary from Elm to JS. So we must ignore any warnings for "kernel" imports.
private val ElmImportClause.safeToIgnore: Boolean
    get() = moduleQID.isKernelModule