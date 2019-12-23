package org.elm.ide.test.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.elm.ide.test.core.ElmProjectTestsHelper
import org.elm.workspace.ElmWorkspaceService

class ElmTestRunConfigurationProducer : LazyRunConfigurationProducer<ElmTestRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return ElmTestConfigurationFactory(ElmTestRunConfigurationType())
    }

    override fun setupConfigurationFromContext(configuration: ElmTestRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        return getCandidateElmFolder(context)
                ?.let {
                    configuration.options.elmFolder = it
                    configuration.setGeneratedName()
                    true
                }
                ?: false
    }

    override fun isConfigurationFromContext(configuration: ElmTestRunConfiguration, context: ConfigurationContext): Boolean {
        return getCandidateElmFolder(context)
                ?.let { it == configuration.options.elmFolder }
                ?: false
    }

    private fun getCandidateElmFolder(context: ConfigurationContext?): String? {
        if (context == null) {
            return null
        }

        val elmWorkspace = ServiceManager.getService(
                context.project, ElmWorkspaceService::class.java)

        return context.location?.virtualFile
                ?.let {
                    elmWorkspace.findProjectForFile(it)
                }?.let {
                    ElmProjectTestsHelper.elmFolderForTesting(it).toString()
                }
    }
}
