package org.elm.ide.livetemplates

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile

class ElmLiveTemplateContext : TemplateContextType("ELM", "Elm") {

    override fun isInContext(file: PsiFile, offset: Int) =
            file.name.endsWith(".elm")
}
