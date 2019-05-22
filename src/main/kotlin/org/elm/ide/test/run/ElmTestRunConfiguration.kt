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
import javax.swing.Icon


class ElmTestRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) : LocatableConfigurationBase<ElmTestRunProfileState>(project, factory, name) {

    internal var options = Options()

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return ElmTestSettingsEditor(project)
    }

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): ElmTestRunProfileState? {
        return ElmTestRunProfileState(executionEnvironment, this)
    }

    //internal
    class Options {
        var elmFolder: String? = null
    }

    override fun getIcon(): Icon? {
        return RUN_ICON
    }

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        this.options = readOptions(element)
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        writeOptions(this.options, element)
    }

    override fun suggestedName(): String? {
        if (options.elmFolder == null) {
            return null
        }
        val elmProjectName = Paths.get(options.elmFolder).fileName
        return "Tests in $elmProjectName"
    }

    companion object {


        // <ElmTestRunConfiguration elm-folder="" elm-test-binary=""/>

        //internal
        fun writeOptions(options: Options, element: Element) {
            val e = Element(ElmTestRunConfiguration::class.java.simpleName)
            if (options.elmFolder != null) {
                e.setAttribute("elm-folder", options.elmFolder)
            }
            element.addContent(e)
        }

        //internal
        fun readOptions(element: Element): Options {
            val result = Options()

            val name = ElmTestRunConfiguration::class.java.simpleName
            val optionsElement = element.getChild(name)

            if (optionsElement != null) {
                val elmFolderAttr = optionsElement.getAttribute("elm-folder")
                result.elmFolder = null
                if (elmFolderAttr != null) {
                    result.elmFolder = elmFolderAttr.value
                }
            }
            return result
        }
    }
}
