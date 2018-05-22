package org.elm.ide.livetemplates

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class ElmLiveTemplateProvider : DefaultLiveTemplatesProvider {
    override fun getDefaultLiveTemplateFiles() = arrayOf("liveTemplates/Elm")

    override fun getHiddenLiveTemplateFiles() = arrayOf<String>()
}