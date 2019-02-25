package org.frawa.elmtest.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.elm.workspace.ElmProject;
import org.elm.workspace.ElmWorkspaceService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public class ElmTestRunConfigurationProducer extends RunConfigurationProducer<ElmTestRunConfiguration> {

    public ElmTestRunConfigurationProducer(@NotNull ElmTestConfigurationFactory configurationFactory) {
        super(configurationFactory);
    }

    public ElmTestRunConfigurationProducer(@NotNull ElmTestRunConfigurationType configurationType) {
        super(configurationType);
    }

    @Override
    protected boolean setupConfigurationFromContext(ElmTestRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        return getCandidateElmFolder(context)
                .map(folder -> {
                    configuration.options.elmFolder = folder;
                    configuration.setGeneratedName();
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean isConfigurationFromContext(ElmTestRunConfiguration configuration, ConfigurationContext context) {
        return getCandidateElmFolder(context)
                .map(folder -> folder.equals(configuration.options.elmFolder))
                .orElse(false);
    }

    private Optional<String> getCandidateElmFolder(ConfigurationContext context) {
        if (context == null) return Optional.empty();

        ElmWorkspaceService elmWorkspace = ServiceManager.getService(
                context.getProject(), ElmWorkspaceService.class);

        return Optional.of(context)
                .map(ConfigurationContext::getLocation)
                .map(Location::getVirtualFile)
                .map(elmWorkspace::findProjectForFile)
                .map(ElmProject::getProjectDirPath)
                .map(Path::toString);
    }
}
