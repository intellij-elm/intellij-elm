package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.elm.lang.core.imports.ImportAdder.Candidate
import org.elm.lang.core.imports.ImportAdder.addImport
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.QualifiedReference
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
                .mapNotNull { fromExposableElement(it, ref) }
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
                    else -> addImport(candidate, file, context.isQualified)
                }
            }
            else -> promptToSelectCandidate(context, file)
        }
    }


    private fun promptToSelectCandidate(context: Context, file: ElmFile) {
        require(context.candidates.isNotEmpty())

        // Put exact matches (i.e. those with `moduleAlias == null`) at the top of the list
        val candidates = context.candidates.sortedWith(
                compareBy<Candidate, String?>(nullsFirst()) { it.moduleAlias }
                        .thenBy { it.moduleName }
        )

        val project = file.project

        val picker = if (isUnitTestMode) {
            MOCK ?: error("You must set mock UI via `withMockUI`")
        } else {
            PickerUI(project, context)
        }
        picker.choose(candidates) { candidate ->
            project.runWriteCommandAction {
                addImport(candidate, file, context.isQualified)
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
 * Returns a candidate if the element is exposed by its containing module
 */
private fun fromExposableElement(element: ElmExposableTag, ref: ElmReference): Candidate? {
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
