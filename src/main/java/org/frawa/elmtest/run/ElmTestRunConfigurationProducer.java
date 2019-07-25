package org.frawa.elmtest.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.elm.workspace.ElmWorkspaceService;
import org.frawa.elmtest.core.ElmProjectTestsHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public class ElmTestRunConfigurationProducer extends LazyRunConfigurationProducer<ElmTestRunConfiguration> {

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return new ElmTestConfigurationFactory(new ElmTestRunConfigurationType());
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
                .flatMap(Optional::ofNullable)
                .map(ElmProjectTestsHelper::elmFolderForTesting)
                .map(Path::toString);
    }
}
