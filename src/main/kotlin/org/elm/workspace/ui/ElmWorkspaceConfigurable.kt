package org.elm.workspace.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import org.elm.openapiext.Result
import org.elm.openapiext.UiDebouncer
import org.elm.openapiext.pathToDirectoryTextField
import org.elm.workspace.ElmToolchain
import org.elm.workspace.elmWorkspace
import javax.swing.JComponent
import javax.swing.JLabel

class ElmWorkspaceConfigurable(
        private val project: Project
) : Configurable, Configurable.NoScroll, Disposable {

    private val versionUpdateDebouncer = UiDebouncer(this)

    private val pathToToolchainField = pathToDirectoryTextField(this,
            "Select directory containing the 'elm' binary") { update() }

    private val elmVersionLabel = JLabel()

    override fun createComponent(): JComponent =
            panel {
                row("Directory containing 'elm' binary:") { pathToToolchainField(CCFlags.pushX) }
                row("Elm version:") { elmVersionLabel() }
            }

    private fun update() {
        val pathToToolchain = pathToToolchainField.text
        versionUpdateDebouncer.run(
                onPooledThread = {
                    ElmToolchain(pathToToolchain).queryCompilerVersion()
                },
                onUiThread = { versionResult ->
                    when (versionResult) {
                        is Result.Ok ->
                            when {
                                versionResult.value < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION -> {
                                    elmVersionLabel.text = "$versionResult.value (not supported)"
                                    elmVersionLabel.foreground = JBColor.RED
                                }
                                else -> {
                                    elmVersionLabel.text = versionResult.value.toString()
                                    elmVersionLabel.foreground = JBColor.foreground()
                                }
                            }
                        is Result.Err -> {
                            elmVersionLabel.text = "error: ${versionResult.reason}"
                            elmVersionLabel.foreground = JBColor.RED
                        }
                    }
                }
        )
    }

    override fun dispose() {
        // needed for the UIDebouncer, but nothing needs to be done here
    }

    override fun disposeUIResources() {
        // needed for Configurable, but nothing needs to be done here
    }

    override fun reset() {
        val settings = project.elmWorkspace.rawSettings
        val binDirPath = settings.binDirPath ?: ElmToolchain.suggest(project)?.binDirPath

        pathToToolchainField.text = binDirPath?.toString() ?: ""

        update()
    }

    override fun apply() {
        project.elmWorkspace.modifySettings {
            it.copy(binDirPath = pathToToolchainField.text)
        }
    }

    override fun isModified(): Boolean {
        val settings = project.elmWorkspace.rawSettings
        return pathToToolchainField.text != settings.binDirPath
    }

    override fun getDisplayName() = "Elm"

    override fun getHelpTopic() = null
}
