package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.imports.ImportAdder
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.endOffset
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.psi.startOffset
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.typeExpressionInference
import org.elm.openapiext.runWriteCommandAction
import org.elm.utils.getIndent

abstract class AnnotationBasedGeneratorIntention : ElmAtCaretIntentionActionBase<AnnotationBasedGeneratorIntention.Context>() {
    data class Context(val file: ElmFile, val ty: Ty, val name: String, val startOffset: Int, val endOffset: Int)

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
        return Context(file, root, typeAnnotation.referenceName, typeAnnotation.startOffset, typeAnnotation.endOffset)
    }

    /** If the intention applies to the type of this annotation, return the [Ty] to use as [Context.ty]. */
    abstract fun getRootIfApplicable(annotationTy: Ty): Ty?

    /** The code generator for this intention*/
    abstract fun generator(context: Context): TyFunctionGenerator

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val generator = generator(context)
        val indent = editor.getIndent(context.startOffset)
        val (generatedCode, imports) = generator.run()
        val code = generatedCode.replace(Regex("\n(?![\r\n])"), "\n$indent")
        project.runWriteCommandAction {
            editor.document.insertString(context.endOffset, "$indent$code")
            if (imports.isNotEmpty()) {
                // Commit the string changes so we can work with the new PSI
                PsiDocumentManager.getInstance(context.file.project).commitDocument(editor.document)
                for (import in imports) {
                    ImportAdder.addImport(import, context.file, import.nameToBeExposed.isEmpty())
                }
            }
        }
    }
}
