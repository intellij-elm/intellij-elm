package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.endOffset
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.typeExpressionInference
import org.elm.openapiext.runWriteCommandAction

abstract class BaseTyGeneratorIntention : ElmAtCaretIntentionActionBase<BaseTyGeneratorIntention.Context>() {
    data class Context(val file: ElmFile, val ty: Ty, val name: String, val endOffset: Int)

    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val file = element.containingFile as? ElmFile ?: return null
        val typeAnnotation = element.parentOfType<ElmTypeAnnotation>()
                ?: return null

        if (typeAnnotation.reference.resolve() != null) {
            // the target declaration already exists; nothing to do
            return null
        }

        val ty = typeAnnotation.typeExpressionInference()?.ty ?: return null
        val root = getRootIfApplicable(ty) ?: return null
        return Context(file, root, typeAnnotation.referenceName, typeAnnotation.endOffset)
    }

    abstract fun getRootIfApplicable(annotationTy: Ty): Ty?
    abstract fun generator(context: Context): TyFunctionGenerator

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val generator = generator(context)
        project.runWriteCommandAction {
            editor.document.insertString(context.endOffset, generator.code)
            if (generator.imports.isNotEmpty()) {
                // Commit the string changes so we can work with the new PSI
                PsiDocumentManager.getInstance(context.file.project).commitDocument(editor.document)
                for (import in generator.imports) {
                    ImportAdder.addImportForCandidate(import, context.file, import.nameToBeExposed.isEmpty())
                }
            }
        }
    }
}
