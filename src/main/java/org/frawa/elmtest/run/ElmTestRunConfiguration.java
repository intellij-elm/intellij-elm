package org.frawa.elmtest.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.frawa.elmtest.run.ElmTestConfigurationFactory.RUN_ICON;

public class ElmTestRunConfiguration extends RunConfigurationBase {

    Options options = new Options();

    protected ElmTestRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new ElmTestSettingsEditor(getProject());
    }

    @Override
    public void checkConfiguration() {
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new ElmTestRunProfileState(executionEnvironment, this);
    }

    static class Options {
        String elmFolder;
        String elmTestBinary;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return RUN_ICON;
    }
}
