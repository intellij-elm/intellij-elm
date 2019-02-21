package org.elm.ide.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.ElmGlslCodeExpr


class ElmGlslInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(ElmGlslCodeExpr::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (!context.isValid || context !is ElmGlslCodeExpr) return

        val language = Language.findInstancesByMimeType("x-shader/x-fragment").firstOrNull() ?: return
        val range = TextRange(7, context.textLength - 2)

        registrar.startInjecting(language)
                .addPlace(null, null, context, range)
                .doneInjecting()
    }
}

