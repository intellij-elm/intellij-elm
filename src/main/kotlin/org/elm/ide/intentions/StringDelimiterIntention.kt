package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmStringConstantExpr

/**
 * Abstract base class for intentions which handle conversions between triple-quoted and regular strings.
 *
 * @param targetDelimiter The delimiter to use in the new string literal created by this intention.
 */
abstract class StringDelimiterIntention(private val targetDelimiter: String) :
    ElmAtCaretIntentionActionBase<StringDelimiterIntention.Context>() {

    data class Context(val stringConstant: ElmStringConstantExpr)

    override fun getFamilyName() = text

    /**
     * Indicates whether `this` [ElmStringConstantExpr] is valid for replacement using this intention.
     */
    protected abstract val ElmStringConstantExpr.isValidForReplacement: Boolean

    /**
     * Gets the string to use to replace `source`, where `source` is the content of the [ElmStringConstantExpr] (i.e.
     * the text inside its delimiters/quotes).
     */
    abstract fun getReplacement(source: String): String

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement) =
        element.ancestors.filterIsInstance<ElmStringConstantExpr>()
            .firstOrNull()
            ?.takeIf { it.isValidForReplacement }
            ?.let { Context(it) }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val newString =
            ElmPsiFactory(project).createStringConstant(
                "$targetDelimiter${getReplacement(context.stringConstant.textContent)}$targetDelimiter"
            )
        context.stringConstant.replace(newString)
    }

    companion object {
        @JvmStatic
        protected val DOUBLE_QUOTE = "\""
        @JvmStatic
        protected val ESCAPED_DOUBLE_QUOTE = "\\\""
        @JvmStatic
        protected val SLASH_N = "\\n"
    }
}
