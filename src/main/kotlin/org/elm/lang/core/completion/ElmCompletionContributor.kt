package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.patterns.PlatformPatterns.psiElement
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmRecordExpr

class ElmCompletionContributor : CompletionContributor() {

    init {
        extend(BASIC, psiElement().withLanguage(ElmLanguage), ElmCompletionProvider())
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val file = context.file as? ElmFile ?: return

        val pre = file.findElementAt(context.startOffset - 1) ?: return

        /*
        By default, IntelliJ injects a dummy identifier that starts with an upper-case letter
        at the insertion caret when doing code completion. This upper-case letter is problematic
        for languages like Elm where many parse rules require a lower-case identifier.
        */

        // TODO use additional context to determine whether it should be upper- vs lower-case.
        context.dummyIdentifier = when {
            // Assume that after a dot, a lower-case identifier should follow.
            // This works well for functions qualified by their module name (e.g. `String.length`)
            // but it does NOT work for qualified types (e.g. `Task.Task`)
            pre.elementType == ElmTypes.DOT -> LOWER_DUMMY_IDENTIFIER
            // Assume lower-case identifiers should be used inside of elm record expressions.
            pre.parent is ElmRecordExpr -> LOWER_DUMMY_IDENTIFIER
            else -> UPPER_DUMMY_IDENTIFIER
        }
    }
}

private const val LOWER_DUMMY_IDENTIFIER = "elm_code_completion_dummy"
private const val UPPER_DUMMY_IDENTIFIER = "Elm_code_completion_dummy"
