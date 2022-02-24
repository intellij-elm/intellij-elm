package org.elm.workspace.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import org.elm.ide.actions.ElmExternalFormatAction
import org.elm.openapiext.Result
import org.elm.openapiext.UiDebouncer
import org.elm.openapiext.fileSystemPathTextField
import org.elm.utils.layout
import org.elm.workspace.*
import org.elm.workspace.commandLineTools.ElmCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmReviewCLI
import org.elm.workspace.commandLineTools.ElmTestCLI
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*

class ElmWorkspaceConfigurable(
        private val project: Project
) : Configurable, Disposable {

    init {
        Disposer.register(project, this)
    }

    private val uiDebouncer = UiDebouncer(this)

    private fun toolPathTextField(programName: String): TextFieldWithBrowseButton {
        return fileSystemPathTextField(this, "Select '$programName'",
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                        .withFileFilter { it.name in ElmSuggest.executableNamesFor(programName) }
                        .also { it.isForcedToUseIdeaFileChooser = true })
        { update() }
    }

    private val elmPathField = toolPathTextField(elmCompilerTool)
    private val elmFormatPathField = toolPathTextField(elmFormatTool)
    private val elmTestPathField = toolPathTextField(elmTestTool)
    private val elmReviewPathField = toolPathTextField(elmReviewTool)

    private val elmVersionLabel = JLabel()
    private val elmFormatVersionLabel = JLabel()
    private val elmFormatOnSaveCheckbox = JCheckBox()
    private val elmFormatShortcutLabel = HyperlinkLabel()
    private val elmTestVersionLabel = JLabel()
    private val elmReviewVersionLabel = JLabel()

    override fun createComponent(): JComponent {
        elmFormatOnSaveCheckbox.addChangeListener { update() }
        elmFormatShortcutLabel.addHyperlinkListener {
            showActionShortcut(ElmExternalFormatAction.ID)
        }

        val panel = layout {
            block("Elm Compiler") {
                row("Location:", pathFieldPlusAutoDiscoverButton(elmPathField, elmCompilerTool))
                row("Version:", elmVersionLabel)
            }
            block(elmFormatTool) {
                row("Location:", pathFieldPlusAutoDiscoverButton(elmFormatPathField, elmFormatTool))
                row("Version:", elmFormatVersionLabel)
                row("Keyboard shortcut:", elmFormatShortcutLabel)
                row("Run when file saved?", elmFormatOnSaveCheckbox)
            }
            block(elmTestTool) {
                row("Location:", pathFieldPlusAutoDiscoverButton(elmTestPathField, elmTestTool))
                row("Version:", elmTestVersionLabel)
            }
            block(elmReviewTool) {
                row("Location:", pathFieldPlusAutoDiscoverButton(elmReviewPathField, elmReviewTool))
                row("Version:", elmReviewVersionLabel)
            }
            block("nvm") {
                val nvmUrl = "https://github.com/nvm-sh/nvm"
                val docsUrl = "https://github.com/klazuka/intellij-elm/blob/master/docs/nvm.md"
                noteRow("""Using <a href="$nvmUrl">nvm</a>? Please read <a href="$docsUrl">our troubleshooting tips</a>.""")
            }
        }

        // Whenever this panel appears, refresh just in case the user made changes on the Keymap settings screen.
        UiNotifyConnector(panel, object : Activatable.Adapter() {
            override fun showNotify() = update()
        }).also { Disposer.register(this, it) }

        return panel
    }

    private fun pathFieldPlusAutoDiscoverButton(field: TextFieldWithBrowseButton, executableName: String): JPanel {
        val panel = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS) }
        with(panel) {
            add(field)
            add(JButton("Auto Discover").apply { addActionListener { field.text = autoDiscoverPathTo(executableName) } })
        }
        return panel
    }

    private fun autoDiscoverPathTo(programName: String) =
            ElmSuggest.suggestTools(project)[programName]?.toString() ?: ""

    private fun showActionShortcut(actionId: String) {
        val dataContext = DataManager.getInstance().getDataContext(elmFormatShortcutLabel)
        val allSettings = Settings.KEY.getData(dataContext) ?: return
        val keymapPanel = allSettings.find(KeymapPanel::class.java) ?: return
        allSettings.select(keymapPanel).doWhenDone {
            keymapPanel.selectAction(actionId)
        }
    }
    data class Results(
            val compilerResult: Result<Version>,
            val elmFormatResult: Result<Version>,
            val elmTestResult: Result<Version>,
            val elmReviewResult: Result<Version>
    )
    
    private fun update() {
        val elmCompilerPath = Paths.get(elmPathField.text)
        val elmFormatPath = Paths.get(elmFormatPathField.text)
        val elmTestPath = Paths.get(elmTestPathField.text)
        val elmReviewPath = Paths.get(elmReviewPathField.text)
        val elmCLI = ElmCLI(elmCompilerPath)
        val elmFormatCLI = ElmFormatCLI(elmFormatPath)
        val elmTestCLI = ElmTestCLI(elmTestPath)
        val elmReviewCLI = ElmReviewCLI(elmReviewPath)
        uiDebouncer.run(
                onPooledThread = {
                    Results(
                            elmCLI.queryVersion(),
                            elmFormatCLI.queryVersion(),
                            elmTestCLI.queryVersion(),
                            elmReviewCLI.queryVersion()
                    )
                },
                onUiThread = { (compilerResult, elmFormatResult, elmTestResult, elmReviewResult) ->
                    with(elmVersionLabel) {
                        when (compilerResult) {
                            is Result.Ok ->
                                when {
                                    compilerResult.value < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION -> {
                                        text = "${compilerResult.value} (not supported)"
                                        foreground = JBColor.RED
                                    }
                                    else -> {
                                        text = compilerResult.value.toString()
                                        foreground = JBColor.foreground()
                                    }
                                }
                            is Result.Err -> {
                                when {
                                    !elmCompilerPath.isValidFor(elmCompilerTool) -> {
                                        text = ""
                                        foreground = JBColor.foreground()
                                    }
                                    else -> {
                                        text = compilerResult.reason
                                        foreground = JBColor.RED
                                    }
                                }
                            }
                        }
                    }

                    with(elmFormatVersionLabel) {
                        when (elmFormatResult) {
                            is Result.Ok -> {
                                text = elmFormatResult.value.toString()
                                foreground = JBColor.foreground()
                            }
                            is Result.Err -> {
                                when {
                                    !elmFormatPath.isValidFor(elmFormatTool) -> {
                                        text = ""
                                        foreground = JBColor.foreground()
                                    }
                                    else -> {
                                        text = elmFormatResult.reason
                                        foreground = JBColor.RED
                                    }
                                }
                            }
                        }
                    }

                    with(elmTestVersionLabel) {
                        when (elmTestResult) {
                            is Result.Ok -> {
                                text = elmTestResult.value.toString()
                                foreground = JBColor.foreground()
                            }
                            is Result.Err -> {
                                when {
                                    !elmTestPath.isValidFor(elmTestTool) -> {
                                        text = ""
                                        foreground = JBColor.foreground()
                                    }
                                    else -> {
                                        text = elmTestResult.reason
                                        foreground = JBColor.RED
                                    }
                                }
                            }
                        }
                    }

                    with(elmReviewVersionLabel) {
                        when (elmReviewResult) {
                            is Result.Ok -> {
                                text = elmReviewResult.value.toString()
                                foreground = JBColor.foreground()
                            }
                            is Result.Err -> {
                                when {
                                    !elmReviewPath.isValidFor(elmReviewTool) -> {
                                        text = ""
                                        foreground = JBColor.foreground()
                                    }
                                    else -> {
                                        text = elmReviewResult.reason
                                        foreground = JBColor.RED
                                    }
                                }
                            }
                        }
                    }
                }
        )


        val shortcuts = KeymapUtil.getActiveKeymapShortcuts(ElmExternalFormatAction.ID).shortcuts
        val shortcutStatus = when {
            shortcuts.isEmpty() -> "No Shortcut"
            else -> shortcuts.joinToString(", ") { KeymapUtil.getShortcutText(it) }
        }
        elmFormatShortcutLabel.setHyperlinkText(shortcutStatus + " ", "Change", "")
    }

    override fun dispose() {
        // needed for the UIDebouncer, but nothing needs to be done here
    }

    override fun disposeUIResources() {
        // needed for Configurable, but nothing needs to be done here
    }

    override fun reset() {
        val settings = project.elmWorkspace.rawSettings
        val elmCompilerPath = settings.elmCompilerPath
        val elmFormatPath = settings.elmFormatPath
        val isElmFormatOnSaveEnabled = settings.isElmFormatOnSaveEnabled
        val elmTestPath = settings.elmTestPath
        val elmReviewPath = settings.elmReviewPath

        elmPathField.text = elmCompilerPath
        elmFormatPathField.text = elmFormatPath
        elmFormatOnSaveCheckbox.isSelected = isElmFormatOnSaveEnabled
        elmTestPathField.text = elmTestPath
        elmReviewPathField.text = elmReviewPath

        update()
    }

    override fun apply() {
        project.elmWorkspace.modifySettings {
            it.copy(elmCompilerPath = elmPathField.text,
                    elmFormatPath = elmFormatPathField.text,
                    elmTestPath = elmTestPathField.text,
                    elmReviewPath = elmReviewPathField.text,
                    isElmFormatOnSaveEnabled = isOnSaveHookEnabledAndSelected()
            )
        }
    }

    private fun isOnSaveHookEnabledAndSelected() =
            elmFormatOnSaveCheckbox.isEnabled && elmFormatOnSaveCheckbox.isSelected

    override fun isModified(): Boolean {
        val settings = project.elmWorkspace.rawSettings
        return elmPathField.text != settings.elmCompilerPath
                || elmFormatPathField.text != settings.elmFormatPath
                || elmTestPathField.text != settings.elmTestPath
                || elmReviewPathField.text != settings.elmReviewPath
                || isOnSaveHookEnabledAndSelected() != settings.isElmFormatOnSaveEnabled
    }

    override fun getDisplayName() = "Elm"

    override fun getHelpTopic() = null
}

private fun Path.isValidFor(programName: String) =
        fileName != null && fileName.toString() in ElmSuggest.executableNamesFor(programName)