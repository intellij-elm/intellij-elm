package org.elm.ide.test.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.elm.ide.icons.ElmIcons

class ElmTestConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project) =
            ElmTestRunConfiguration(project, this, "Elm Test")

    override fun getName() = "Elm Test configuration factory"

    override fun getIcon() = RUN_ICON

    override fun getId(): String = "ELM_TEST_RUN_CONFIGURATION"

    companion object {
        val RUN_ICON = ElmIcons.COLORFUL
    }
}