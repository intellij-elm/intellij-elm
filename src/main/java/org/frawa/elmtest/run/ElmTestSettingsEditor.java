// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.frawa.elmtest.run;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.elm.workspace.ElmProject;
import org.elm.workspace.ElmWorkspaceService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

public class ElmTestSettingsEditor extends SettingsEditor<ElmTestRunConfiguration> {
    private final List<ElmProject> elmProjects;
    private JPanel myPanel;
    private ComboBox<String> projectChooser;

    ElmTestSettingsEditor(Project project) {
        ElmWorkspaceService workspaceService = ServiceManager.getService(project, ElmWorkspaceService.class);
        elmProjects = workspaceService.getAllProjects();

        elmProjects.stream()
                .map(ElmProject::getPresentableName)
                .forEach(projectChooser::addItem);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    @Override
    protected void resetEditorFrom(@NotNull ElmTestRunConfiguration configuration) {
        Optional<String> name = elmProjects.stream()
                .filter(p -> p.getProjectDirPath().toString().equals(configuration.options.elmFolder))
                .map(ElmProject::getPresentableName)
                .findFirst();
        name.ifPresent(projectChooser::setSelectedItem);
    }

    @Override
    protected void applyEditorTo(@NotNull ElmTestRunConfiguration configuration) {
        int index = projectChooser.getSelectedIndex();
        if (index >= 0) {
            configuration.options.elmFolder = elmProjects.get(index).getProjectDirPath().toString();
        }
    }
}
