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

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ElmTestSettingsEditor extends SettingsEditor<ElmTestRunConfiguration> {
    private JPanel myPanel;
    private TextFieldWithBrowseButton folderChooser;
    private TextFieldWithBrowseButton elmTestChooser;

    public ElmTestSettingsEditor(Project project) {
        FileChooserDescriptor elmFolderDescriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor();
        folderChooser.addBrowseFolderListener("Elm Project Folder", null, project, elmFolderDescriptor);

        FileChooserDescriptor elmBinaryDescriptor = FileChooserDescriptorFactory
                .createSingleFileDescriptor();
        elmTestChooser.addBrowseFolderListener("elm-test Binary", null, project, elmBinaryDescriptor);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    @Override
    protected void resetEditorFrom(@NotNull ElmTestRunConfiguration configuration) {
        folderChooser.setText(configuration.options.elmFolder);
        elmTestChooser.setText(configuration.options.elmTestBinary);
    }

    @Override
    protected void applyEditorTo(@NotNull ElmTestRunConfiguration configuration) {
        configuration.options.elmFolder = folderChooser.getText();
        configuration.options.elmTestBinary = elmTestChooser.getText();
    }
}
