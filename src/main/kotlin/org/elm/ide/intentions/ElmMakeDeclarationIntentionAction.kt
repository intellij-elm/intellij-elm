package org.elm.ide.intentions

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType

class ElmMakeDeclarationIntentionAction : ElmAtCaretIntentionActionBase<ElmMakeDeclarationIntentionAction.Context>() {

    data class Context(val typeAnnotation: ElmTypeAnnotation)

    override fun getText() = "Create"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val typeAnnotation = element.parentOfType<ElmTypeAnnotation>()
                ?: return null

        if (typeAnnotation.reference.resolve() != null) {
            // the target declaration already exists; nothing needs to be done
            return null
        }

        return Context(typeAnnotation)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        object : WriteCommandAction.Simple<Unit>(project) {
            override fun run() {
                generateDecl(project, editor, context)
            }
        }.execute()
    }

    private fun generateDecl(project: Project, editor: Editor, context: Context) {
        val typeAnnotation = context.typeAnnotation
        val factory = ElmPsiFactory(project)

        typeAnnotation.parent.addAfter(factory.createFreshLine(), typeAnnotation)
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        editor.caretModel.moveCaretRelatively(0, 1, false, false, false)

        val template = generateTemplate(project, context)
        TemplateManager.getInstance(project).startTemplate(editor, template)
    }

    private fun generateTemplate(project: Project, context: Context): Template {
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        template.isToReformat = false

        val typeAnnotation = context.typeAnnotation
        val name = typeAnnotation.referenceName
        template.addTextSegment("$name ")

        val typeRef = typeAnnotation.typeRef
        val args: List<String> = if (typeRef == null) {
            emptyList()
        } else {
            typeRef.children.dropLast(1).map {
                when (it) {
                    is ElmTypeVariableRef -> it.text
                    is ElmParametricTypeRef -> it.upperCaseQID.text
                    is ElmRecordType -> "record"
                    is ElmTupleType -> "tuple"
                    is ElmTypeRef -> "function" // not quite true: need to check for an ARROW child
                    else -> "?"
                }.toLowerCamelCase()
            }
        }

        for (arg in args) {
            template.addVariable(TextExpression(arg), true)
            template.addTextSegment(" ")
        }

        template.addTextSegment("= ")
        template.addEndVariable()
        return template
    }
}

/// NOTE: Assumes that the receiver is already InterCapped.
private fun String.toLowerCamelCase(): String =
        this.mapIndexed { idx, c ->
            if (idx == 0)
                c.toLowerCase()
            else
                c
        }.joinToString("")
