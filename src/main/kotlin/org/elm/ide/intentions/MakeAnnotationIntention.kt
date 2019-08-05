package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.findTy
import org.elm.lang.core.types.renderedText
import org.elm.openapiext.runWriteCommandAction
import org.elm.utils.getIndent

class MakeAnnotationIntention : ElmAtCaretIntentionActionBase<MakeAnnotationIntention.Context>() {

    data class Context(val fdl: ElmFunctionDeclarationLeft, val valueDeclaration: ElmValueDeclaration, val ty: Ty)

    override fun getText() = "Add type annotation"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val fdl = element.parentOfType<ElmFunctionDeclarationLeft>()
                ?: return null

        val declaration = fdl.parentOfType<ElmValueDeclaration>()
                ?: return null

        if (declaration.typeAnnotation != null) {
            // the target annotation already exists; nothing needs to be done
            return null
        }

        val ty = declaration.findTy() ?: return null

        return Context(fdl, declaration, ty)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val (fdl, valueDeclaration, ty) = context
        val indent = editor.getIndent(valueDeclaration.startOffset)
        val code = "${fdl.name} : ${ty.renderedText(elmFile = fdl.elmFile).replace("→", "->")}\n$indent"
        project.runWriteCommandAction {
            editor.document.insertString(valueDeclaration.startOffset, code)
        }
    }
}
