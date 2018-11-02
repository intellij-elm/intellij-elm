package org.frawa.elmtest.run.configuration;

import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ElmTestSettingsEditor extends SettingsEditor<ElmTestRunConfiguration> {

    @Override
    protected void resetEditorFrom(@NotNull ElmTestRunConfiguration s) {

    }

    @Override
    protected void applyEditorTo(@NotNull ElmTestRunConfiguration s) {
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return new JPanel();
    }
}
