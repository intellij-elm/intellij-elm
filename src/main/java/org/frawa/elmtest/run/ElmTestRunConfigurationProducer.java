package org.frawa.elmtest.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.frawa.elmtest.core.ElmProjectTestsHelper;
import org.jetbrains.annotations.NotNull;

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
        return Optional
                .ofNullable(context)
                .map(ConfigurationContext::getModule)
                .map(Module::getModuleFile)
                .map(VirtualFile::getParent)
                .map(VirtualFile::getPath)
                .filter(candidate -> ElmProjectTestsHelper.isElmProject(candidate, context.getProject()));
    }
}
