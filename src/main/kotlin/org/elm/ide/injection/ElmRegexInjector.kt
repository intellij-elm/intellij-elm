package org.elm.ide.injection

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmStringConstantExpr
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.prevSiblings
import org.elm.lang.core.psi.withoutWs
import org.intellij.lang.regexp.RegExpLanguage

private val commentRegex = Regex("(?i)^(?:\\{-|--)\\s*language\\s*[:=]\\s*regexp?\\s*(?:-})?$")


class ElmRegexInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(ElmStringConstantExpr::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (!context.isValid || context !is ElmStringConstantExpr || !shouldInject(context)) return

        val text = context.text
        val range = when {
            text.startsWith("\"\"\"") -> TextRange(3, text.length - 3)
            else -> TextRange(1, text.length - 1)
        }

        registrar.startInjecting(RegExpLanguage.INSTANCE)
                .addPlace(null, null, context, range)
                .doneInjecting()
    }

    private fun shouldInject(context: ElmStringConstantExpr): Boolean {
        return isRegexFromStringArg(context) || hasRegexLangComment(context)
    }

    // This function is called on the EDT, so resolving references can freeze the UI. Instead, we
    // just have to check the function name. This means that we won't inject unless the function is
    // called qualified with the Regex module.
    private fun isRegexFromStringArg(context: ElmStringConstantExpr): Boolean {
        val call = context.parent as? ElmFunctionCallExpr ?: return false
        val targetName = call.target.text
        // We don't need to check which argument of the call we're looking at, since both of these
        // functions only take one string
        return targetName == "Regex.fromString" || targetName == "Regex.fromStringWith"
    }

    private fun hasRegexLangComment(context: ElmStringConstantExpr): Boolean {
        val comment = getPrevComment(context) ?: getValueDeclComment(context) ?: return false
        return comment.text.matches(commentRegex)
    }

    private fun getPrevComment(element: PsiElement): PsiComment? {
        return element.prevSiblings.withoutWs.firstOrNull() as? PsiComment
    }

    /** If [element] is a direct child of a value declaration, return the declaration's comment */
    private fun getValueDeclComment(element: PsiElement): PsiComment? {
        val decl = element.parent as? ElmValueDeclaration ?: return null
        return decl.prevSiblings.withoutWs.filter { it !is ElmTypeAnnotation }
                .firstOrNull() as? PsiComment
    }
}

