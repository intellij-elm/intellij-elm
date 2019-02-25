package org.frawa.elmtest.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.elm.ide.icons.ElmIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ElmTestConfigurationFactory extends ConfigurationFactory {

    private static final String FACTORY_NAME = "Elm Test configuration factory";

    static final Icon RUN_ICON = ElmIcons.INSTANCE.getCOLORFUL();

    ElmTestConfigurationFactory(ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new ElmTestRunConfiguration(project, this, "Elm Test");
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return RUN_ICON;
    }

}