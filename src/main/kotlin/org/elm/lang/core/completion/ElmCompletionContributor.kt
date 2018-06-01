package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.patterns.PlatformPatterns.psiElement
import org.elm.lang.core.ElmLanguage

class ElmCompletionContributor : CompletionContributor() {

    init {
        extend(BASIC, psiElement().withLanguage(ElmLanguage), ElmCompletionProvider())
    }
}
