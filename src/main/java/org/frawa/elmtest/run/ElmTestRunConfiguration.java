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
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.frawa.elmtest.run.ElmTestConfigurationFactory.RUN_ICON;

public class ElmTestRunConfiguration extends RunConfigurationBase {

    final Options options = new Options();

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


    // <ElmTestRunConfiguration elm-folder="" elm-test-binary=""/>

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        String name = ElmTestRunConfiguration.class.getSimpleName();
        Element settingsElement = element.getChild(name);
        //if it is in wrong format (e.g., old with different class or tag name, or missing data from experiments), missing this check will produce a nullpointer and user loses all run definitions
        if (settingsElement == null) return;
        //not sure if you can create a new instance or not but just to be sure I don't
        Attribute elmFolderAttr = settingsElement.getAttribute("elm-folder");
        options.elmFolder = null;
        if (elmFolderAttr != null) {
            options.elmFolder = elmFolderAttr.getValue();
        }
        Attribute elmTestBinAttr = settingsElement.getAttribute("elm-test-binary");
        options.elmTestBinary = null;
        if (elmTestBinAttr != null) {
            options.elmTestBinary = elmTestBinAttr.getValue();
        }


    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        //the tag name in the XML file will be the class name of runParameters
        Element e = new Element(ElmTestRunConfiguration.class.getSimpleName());
        if (options.elmFolder != null) {
            e.setAttribute("elm-folder", options.elmFolder);
        }
        if (options.elmTestBinary != null) {
            e.setAttribute("elm-test-binary", options.elmTestBinary);
        }
        element.addContent(e);
    }
}
