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
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.nextLeaves
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.renderParam
import org.elm.lang.core.types.typeExpressionInference

class MakeDeclarationIntention : ElmAtCaretIntentionActionBase<MakeDeclarationIntention.Context>() {

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
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            generateDecl(project, editor, context)
        }
    }

    private fun generateDecl(project: Project, editor: Editor, context: Context) {
        val typeAnnotation = context.typeAnnotation
        val factory = ElmPsiFactory(project)

        // Insert a newline at the end of this line
        val anchor = typeAnnotation.nextLeaves
                .takeWhile { !it.text.contains('\n') }
                .lastOrNull() ?: typeAnnotation
        typeAnnotation.parent.addAfter(factory.createFreshLine(), anchor)
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

        // Move the caret down to the line that we just created
        editor.caretModel.moveCaretRelatively(0, 1, false, false, false)

        // Insert the Live Template
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

        val ty = typeAnnotation.typeExpressionInference()?.ty
        val args: List<String> = when (ty) {
            is TyFunction -> ty.parameters.map { it.renderParam() }
            else -> emptyList()
        }

        for (arg in args) {
            template.addVariable(TextExpression(arg), true)
            template.addTextSegment(" ")
        }

        template.addTextSegment("=\n    ")
        template.addEndVariable()
        return template
    }
}
