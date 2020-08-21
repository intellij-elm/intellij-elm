package org.elm.ide.inspections.import

import com.intellij.codeInsight.intention.PriorityAction.Priority
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.text.EditDistance
import org.elm.ide.inspections.NamedQuickFix
import org.elm.lang.core.imports.ImportAdder.Import
import org.elm.lang.core.imports.ImportAdder.addImport
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.QualifiedReference
import org.elm.openapiext.isUnitTestMode
import org.elm.openapiext.runWriteCommandAction
import org.jetbrains.annotations.TestOnly
import javax.swing.JList


interface ImportPickerUI {
    fun choose(candidates: List<Import>, callback: (Import) -> Unit)
}

private var MOCK: ImportPickerUI? = null

@TestOnly
fun withMockImportPickerUI(mockUi: ImportPickerUI, action: () -> Unit) {
    MOCK = mockUi
    try {
        action()
    } finally {
        MOCK = null
    }
}

class AddImportFix : NamedQuickFix("Import", Priority.HIGH) {
    data class Context(
            val refName: String,
            val candidates: List<Import>,
            val isQualified: Boolean
    )

    companion object {
        fun findApplicableContext(psiElement: PsiElement): Context? {
            val element = psiElement as? ElmPsiElement ?: return null
            if (element.parentOfType<ElmImportClause>() != null) return null
            val refElement = element.parentOfType<ElmReferenceElement>(strict = false) ?: return null
            val ref = refElement.reference

            // we can't import the function we're annotating
            if (refElement is ElmTypeAnnotation) return null

            val typeAllowed = element is ElmTypeRef
            val name = refElement.referenceName
            val candidates = ElmLookup.findByName<ElmExposableTag>(name, refElement.elmFile)
                    .filter {
                        val isType = it is ElmTypeDeclaration || it is ElmTypeAliasDeclaration
                        typeAllowed == isType
                    }
                    .mapNotNull { fromExposableElement(it, ref) }
                    .sortedWith(referenceComparator(ref))

            if (candidates.isEmpty())
                return null

            return Context(name, candidates, ref is QualifiedReference)
        }

        private fun referenceComparator(ref: ElmReference): Comparator<Import> {
            val qualifier = (ref as? QualifiedReference)?.qualifierPrefix
            val comparator = compareBy<Import, String?>(nullsFirst()) { it.moduleAlias }
            return when {
                // With no qualifier, just sort lexicographically
                qualifier.isNullOrBlank() -> comparator.thenBy { it.moduleName }
                else -> comparator
                        // Sort by modules containing the qualifier exactly
                        .thenByDescending { qualifier in it.moduleName }
                        // Next sort by the case-insensitive edit distance
                        .thenBy { EditDistance.levenshtein(qualifier, it.moduleName, /*caseSensitive=*/false) }
                        // Finally sort by case-sensitive edit distance, so exact case matches sort higher
                        .thenBy { EditDistance.levenshtein(qualifier, it.moduleName, /*caseSensitive=*/true) }
            }
        }
    }

    override fun applyFix(element: PsiElement, project: Project) {
        if (element !is ElmPsiElement) return
        val file = element.elmFile
        val context = findApplicableContext(element) ?: return
        when (context.candidates.size) {
            0 -> error("should not happen: must be at least one candidate")
            1 -> {
                val candidate = context.candidates.first()
                // Normally we would just directly perform the import here without prompting,
                // but if it's an alias-based import, we should show the user some UI so that
                // they know what they're getting into. See https://github.com/klazuka/intellij-elm/issues/309
                when {
                    candidate.moduleAlias != null -> promptToSelectCandidate(project, context, file)
                    else -> project.runWriteCommandAction {
                        addImport(candidate, file, context.isQualified)
                    }
                }
            }
            else -> promptToSelectCandidate(project, context, file)
        }
    }

    private fun promptToSelectCandidate(project: Project, context: Context, file: ElmFile) {
        require(context.candidates.isNotEmpty())
        DataManager.getInstance().dataContextFromFocusAsync.onSuccess { dataContext ->
            val picker = if (isUnitTestMode) {
                MOCK ?: error("You must set mock UI via `withMockImportPickerUI`")
            } else {
                RealImportPickerUI(dataContext, context.refName, project)
            }
            picker.choose(context.candidates) { candidate ->
                project.runWriteCommandAction {
                    addImport(candidate, file, context.isQualified)
                }
            }
        }
    }
}

private class RealImportPickerUI(
        private val dataContext: DataContext,
        private val refName: String,
        private val project: Project
) : ImportPickerUI {
    override fun choose(candidates: List<Import>, callback: (Import) -> Unit) {
        val popup = JBPopupFactory.getInstance().createPopupChooserBuilder(candidates)
                .setTitle("Import '$refName' from module:")
                .setItemChosenCallback { callback(it) }
                .setNamerForFiltering { it.moduleName }
                .setRenderer(CandidateRenderer())
                .createPopup()
        NavigationUtil.hidePopupIfDumbModeStarts(popup, project)
        popup.showInBestPositionFor(dataContext)
    }
}

private class CandidateRenderer : ColoredListCellRenderer<Import>() {
    override fun customizeCellRenderer(list: JList<out Import>, value: Import, index: Int, selected: Boolean, hasFocus: Boolean) {
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
 * Returns a candidate if the element is exposed by its containing module
 */
private fun fromExposableElement(element: ElmExposableTag, ref: ElmReference): Import? {
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

        else ->
            element.name
    }

    val alias = inferModuleAlias(ref, moduleDecl)
    if (alias != null && alias.contains('.')) {
        // invalid candidate because the alias would violate Elm syntax
        return null
    }

    return Import(
            moduleName = moduleDecl.name,
            moduleAlias = alias,
            nameToBeExposed = nameToBeExposed
    )
}

/**
 * Attempt to infer an alias to be used when importing this module.
 */
private fun inferModuleAlias(ref: ElmReference, moduleDecl: ElmModuleDeclaration): String? =
        if (ref is QualifiedReference && ref.qualifierPrefix != moduleDecl.name)
            ref.qualifierPrefix
        else
            null
