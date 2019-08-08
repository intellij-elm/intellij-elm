package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.elements.Flavor.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.resolve.scope.VisibleNames
import org.elm.lang.core.types.moduleName
import org.elm.openapiext.isUnitTestMode
import org.elm.openapiext.runWriteCommandAction
import org.elm.openapiext.toPsiFile
import org.jetbrains.annotations.TestOnly

class AddQualifierIntention : ElmAtCaretIntentionActionBase<AddQualifierIntention.Context>() {

    data class Context(
            val candidates: List<String>,
            val referenceName: String,
            val qid: ElmQID
    )

    override fun getText() = "Qualify name"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val file = editor.toPsiFile(project) as? ElmFile ?: error("no file: should not happen")
        if (element.ancestors.any { it is ElmModuleDeclaration || it is ElmPortAnnotation || it is ElmImportClause }) {
            return null
        }

        val qid = element.parentOfType<ElmQID>() ?: return null
        if (qid.isQualified) return null

        val typeRef = qid.parentOfType<ElmTypeRef>()
        if (typeRef != null) {
            return makeContext(file, qid, typeRef, ModuleScope.getReferencableTypes(file))
        }

        val valueRef = qid.parentOfType<ElmValueExpr>() ?: return null

        return when (valueRef.flavor) {
            QualifiedValue, QualifiedConstructor -> null
            BareValue -> {
                makeContext(file, qid, valueRef, ModuleScope.getReferencableValues(file))
            }
            BareConstructor -> {
                makeContext(file, qid, valueRef, ModuleScope.getReferencableConstructors(file))
            }
        }
    }

    private fun makeContext(file: ElmFile, qid: ElmQID, ref: ElmReferenceElement, names: VisibleNames): Context? {
        val name = ref.referenceName
        val candidates = (names.global + names.imported)
                .filter { it.name == name }
                .mapNotNull { ModuleScope.getQualifierForName(file, it.moduleName, name) }
                .filter { it.isNotEmpty() }
        if (candidates.isEmpty()) return null
        return Context(candidates, name, qid)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        when (context.candidates.size) {
            0 -> error("should not happen: must be at least one candidate")
            1 -> addQualifier(project, context, context.candidates.first())
            else -> promptToSelectCandidate(project, editor, context)
        }
    }

    private fun promptToSelectCandidate(project: Project, editor: Editor, context: Context) {
        require(context.candidates.isNotEmpty())

        val picker = if (isUnitTestMode) {
            MOCK ?: error("You must set mock UI via `withMockQualifierPickerUI`")
        } else {
            RealQualifierPickerUI(editor, context)
        }
        picker.choose(context.candidates) { qualifier ->
            project.runWriteCommandAction {
                addQualifier(project, context, qualifier)
            }
        }
    }

    private fun addQualifier(project: Project, context: Context, qualifier: String) {
        val factory = ElmPsiFactory(project)
        val newName = qualifier + context.referenceName
        val newId = when (context.qid) {
            is ElmUpperCaseQID -> factory.createUpperCaseQID(newName)
            is ElmValueQID -> factory.createValueQID(newName)
            else -> error("unexpected QID type")
        }
        context.qid.replace(newId)
    }
}

interface QualifierPickerUI {
    fun choose(qualifiers: List<String>, callback: (String) -> Unit)
}

private var MOCK: QualifierPickerUI? = null

@TestOnly
fun withMockQualifierPickerUI(mockUi: QualifierPickerUI, action: () -> Unit) {
    MOCK = mockUi
    try {
        action()
    } finally {
        MOCK = null
    }
}

private class RealQualifierPickerUI(val editor: Editor, val context: AddQualifierIntention.Context) : QualifierPickerUI {
    override fun choose(qualifiers: List<String>, callback: (String) -> Unit) {
        JBPopupFactory.getInstance().createPopupChooserBuilder(qualifiers)
                .setTitle("Add qualifier to '${context.referenceName}':")
                .setItemChosenCallback { callback(it) }
                .setNamerForFiltering { it }
                .createPopup().showInBestPositionFor(editor)
    }
}
