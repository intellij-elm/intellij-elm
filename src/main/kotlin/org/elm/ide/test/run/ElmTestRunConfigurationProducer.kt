package org.elm.ide.test.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.elm.workspace.elmWorkspace

class ElmTestRunConfigurationProducer : LazyRunConfigurationProducer<ElmTestRunConfiguration>() {

    override fun getConfigurationFactory() =
            ElmTestConfigurationFactory(ElmTestRunConfigurationType())

    override fun setupConfigurationFromContext(configuration: ElmTestRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val elmFolder = getCandidateElmFolder(context) ?: return false
        configuration.options.elmFolder = elmFolder
        configuration.setGeneratedName()
        return true
    }

    override fun isConfigurationFromContext(configuration: ElmTestRunConfiguration, context: ConfigurationContext): Boolean {
        val elmFolder = getCandidateElmFolder(context) ?: return false
        return elmFolder == configuration.options.elmFolder
    }

    private fun getCandidateElmFolder(context: ConfigurationContext): String? {
        val vfile = context.location?.virtualFile ?: return null
        val elmProject = context.project.elmWorkspace.findProjectForFile(vfile) ?: return null
        return elmProject.projectDirPath.toString()
    }
}
