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
package org.elm.ide.test.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import org.elm.ide.test.core.ElmProjectTestsHelper
import javax.swing.JComponent
import javax.swing.JPanel

class ElmTestSettingsEditor internal constructor(project: Project) : SettingsEditor<ElmTestRunConfiguration>() {
    private val helper: ElmProjectTestsHelper = ElmProjectTestsHelper(project)
    private var myPanel: JPanel? = null
    private var projectChooser: ComboBox<String>? = null

    override fun createEditor(): JComponent {
        helper.allNames().forEach { projectChooser!!.addItem(it) }
        return this.myPanel!!
    }

    override fun resetEditorFrom(configuration: ElmTestRunConfiguration) {
        this.projectChooser!!.selectedItem =
                configuration.options.elmFolder?.let {
                    helper.nameByProjectDirPath(it)
                }
    }

    override fun applyEditorTo(configuration: ElmTestRunConfiguration) {
        val name : String? = projectChooser!!.selectedItem as String
        if (name != null) {
            configuration.options.elmFolder = helper.projectDirPathByName(name)
        }
    }
}
