package org.frawa.elmtest.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.frawa.elmtest.run.ElmTestConfigurationFactory.RUN_ICON;

public class ElmTestRunConfiguration extends LocatableConfigurationBase<ElmTestRunProfileState> {

    Options options = new Options();

    ElmTestRunConfiguration(Project project, ConfigurationFactory factory, String name) {
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
    public ElmTestRunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new ElmTestRunProfileState(executionEnvironment, this);
    }

    static class Options {
        String elmFolder;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return RUN_ICON;
    }


    // <ElmTestRunConfiguration elm-folder="" elm-test-binary=""/>

    static void writeOptions(Options options, Element element) {
        Element e = new Element(ElmTestRunConfiguration.class.getSimpleName());
        if (options.elmFolder != null) {
            e.setAttribute("elm-folder", options.elmFolder);
        }
        element.addContent(e);
    }

    static Options readOptions(Element element) {
        Options result = new Options();

        String name = ElmTestRunConfiguration.class.getSimpleName();
        Element optionsElement = element.getChild(name);

        if (optionsElement != null) {
            Attribute elmFolderAttr = optionsElement.getAttribute("elm-folder");
            result.elmFolder = null;
            if (elmFolderAttr != null) {
                result.elmFolder = elmFolderAttr.getValue();
            }
        }
        return result;
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        this.options = readOptions(element);
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        writeOptions(this.options, element);
    }

    @Nullable
    @Override
    public String suggestedName() {
        if (options.elmFolder == null) {
            return null;
        }
        Path elmProjectName = Paths.get(options.elmFolder).getFileName();
        return elmProjectName != null ? "Tests in " + elmProjectName.toString() : null;
    }
}
