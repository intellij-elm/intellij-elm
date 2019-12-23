package org.elm.ide.test.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import org.elm.ide.test.run.ElmTestConfigurationFactory.Companion.RUN_ICON
import javax.swing.Icon

class ElmTestRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String {
        return "Elm Test"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Elm Test Runner"
    }

    override fun getIcon(): Icon {
        return RUN_ICON
    }

    override fun getId(): String {
        return "ELM_TEST_RUN_CONFIGURATION"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(ElmTestConfigurationFactory(this))
    }


}

