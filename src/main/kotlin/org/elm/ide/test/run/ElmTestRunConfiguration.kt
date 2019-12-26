package org.elm.ide.test.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.elm.ide.test.run.ElmTestConfigurationFactory.Companion.RUN_ICON
import org.jdom.Element
import java.nio.file.Paths


class ElmTestRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) : LocatableConfigurationBase<ElmTestRunProfileState>(project, factory, name) {

    var options = Options()

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
            ElmTestSettingsEditor(project)

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
            ElmTestRunProfileState(executionEnvironment, this)

    class Options {
        var elmFolder: String? = null
    }

    override fun getIcon() = RUN_ICON

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        options = readOptions(element)
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        writeOptions(options, element)
    }

    override fun suggestedName(): String? {
        val elmFolder = options.elmFolder ?: return null
        return "Tests in ${Paths.get(elmFolder).fileName}"
    }

    companion object {

        // <ElmTestRunConfiguration elm-folder="" />

        fun writeOptions(options: Options, element: Element) {
            val e = Element(ElmTestRunConfiguration::class.java.simpleName)
            if (options.elmFolder != null) {
                e.setAttribute("elm-folder", options.elmFolder)
            }
            element.addContent(e)
        }

        fun readOptions(element: Element): Options {
            return Options().apply {
                val name = ElmTestRunConfiguration::class.java.simpleName
                elmFolder = element.getChild(name)?.getAttribute("elm-folder")?.value
            }
        }
    }
}
