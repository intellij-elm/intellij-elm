package org.elm.ide.test.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.elm.ide.icons.ElmIcons
import javax.swing.Icon

class ElmTestConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return ElmTestRunConfiguration(project, this, "Elm Test")
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    override fun getIcon(): Icon? {
        return RUN_ICON
    }

    companion object {

        private val FACTORY_NAME = "Elm Test configuration factory"

//        internal
        val RUN_ICON = ElmIcons.COLORFUL
    }

}