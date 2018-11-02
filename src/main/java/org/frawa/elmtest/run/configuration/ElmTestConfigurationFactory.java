package org.frawa.elmtest.run.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

public class ElmTestConfigurationFactory extends ConfigurationFactory {

    private static final String FACTORY_NAME = "Elm Test configuration factory";

    protected ElmTestConfigurationFactory(ConfigurationType type) {
        super(type);
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
        return new ElmTestRunConfiguration(project, this, "Elm Test");
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }
}