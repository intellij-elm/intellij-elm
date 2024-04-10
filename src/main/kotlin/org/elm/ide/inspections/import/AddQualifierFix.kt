package org.elm.ide.inspections.import

import com.intellij.codeInsight.intention.PriorityAction.Priority
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import org.elm.ide.inspections.NamedQuickFix
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.elements.Flavor.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.resolve.scope.VisibleNames
import org.elm.openapiext.isUnitTestMode
import org.elm.openapiext.runWriteCommandAction
import org.jetbrains.annotations.TestOnly

class AddQualifierFix : NamedQuickFix("Qualify name", Priority.HIGH) {

    data class Context(
            val candidates: List<String>,
            val referenceName: String,
            val qid: ElmQID
    )

    companion object {
        fun findApplicableContext(psiElement: PsiElement?): Context? {
            val element = psiElement as? ElmPsiElement ?: return null

            if (element.ancestors.any { it is ElmModuleDeclaration || it is ElmPortAnnotation || it is ElmImportClause }) {
                return null
            }

            val file = element.elmFile

            return when (element) {
                is ElmTypeRef -> makeContext(file, element.upperCaseQID, element, ModuleScope.getReferencableTypes(file))
                is ElmUnionPattern -> makeContext(file, element.upperCaseQID, element, ModuleScope.getReferencableConstructors(file))
                is ElmValueExpr -> when (element.flavor) {
                    QualifiedValue, QualifiedConstructor -> null
                    BareValue -> {
                        makeContext(file, element.qid, element, ModuleScope.getReferencableValues(file))
                    }
                    BareConstructor -> {
                        makeContext(file, element.qid, element, ModuleScope.getReferencableConstructors(file))
                    }
                }
                else -> null
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
    }

    override fun applyFix(element: PsiElement, project: Project) {
        val context = findApplicableContext(element) ?: return

        when (context.candidates.size) {
            0 -> error("should not happen: must be at least one candidate")
            1 -> project.runWriteCommandAction {
                addQualifier(project, context, context.candidates.first())
            }
            else -> promptToSelectCandidate(project, context)
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

    private fun promptToSelectCandidate(project: Project, context: Context) {
        require(context.candidates.isNotEmpty())

        DataManager.getInstance().dataContextFromFocusAsync.onSuccess { dataContext ->
            val picker = if (isUnitTestMode) {
                MOCK ?: error("You must set mock UI via `withMockQualifierPickerUI`")
            } else {
                RealQualifierPickerUI(dataContext, context)
            }
            picker.choose(context.candidates) { qualifier ->
                project.runWriteCommandAction {
                    addQualifier(project, context, qualifier)
                }
            }
        }
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

private class RealQualifierPickerUI(val dataContext: DataContext, val context: AddQualifierFix.Context) : QualifierPickerUI {
    override fun choose(qualifiers: List<String>, callback: (String) -> Unit) {
        JBPopupFactory.getInstance().createPopupChooserBuilder(qualifiers)
                .setTitle("Add qualifier to '${context.referenceName}':")
                .setItemChosenCallback { callback(it) }
                .setNamerForFiltering { it }
                .createPopup().showInBestPositionFor(dataContext)
    }
}
