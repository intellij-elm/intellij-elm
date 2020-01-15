package org.elm.ide.formatter.settings

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.*
import org.elm.lang.core.ElmLanguage


class ElmCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun getConfigurableDisplayName() = ElmLanguage.displayName

    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(settings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
                    ElmCodeStyleMainPanel(currentSettings, settings)
        }
    }

    private class ElmCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
            TabbedLanguageCodeStylePanel(ElmLanguage, currentSettings, settings) {

        override fun initTabs(settings: CodeStyleSettings?) {
            addIndentOptionsTab(settings)
        }
    }
}

class ElmLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = ElmLanguage

    // No sample, since we don't have a formatter.
    // An empty string causes an NPE, so we use a zero-width space instead.
    override fun getCodeSample(settingsType: SettingsType) = "\u200B"

    // no setting other than indent yet
    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {}

    // If we ever add formatting support, we'll probably want to return a plain
    // `SmartIndentOptionsEditor()` rather than this custom one.
    override fun getIndentOptionsEditor(): IndentOptionsEditor? = ElmOptionsEditor()
}


private class ElmOptionsEditor : IndentOptionsEditor() {
    // Only expose the indent field. Setting this in `customizeSettings` doesn't seem to have an
    // effect.
    override fun addComponents() {
        super.addComponents()
        showStandardOptions("INDENT_SIZE")
    }
}
