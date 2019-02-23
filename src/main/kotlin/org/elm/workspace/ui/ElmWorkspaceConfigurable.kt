package org.elm.workspace.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import org.elm.ide.actions.ElmExternalFormatAction
import org.elm.openapiext.Result
import org.elm.openapiext.UiDebouncer
import org.elm.openapiext.fileSystemPathTextField
import org.elm.workspace.*
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ElmWorkspaceConfigurable(
        private val project: Project
) : Configurable, Configurable.NoScroll, Disposable {

    private val uiDebouncer = UiDebouncer(this)

    private val elmPathField =
            fileSystemPathTextField(this, "Select 'elm'",
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                            .withFileFilter { it.name in ElmSuggest.executableNamesFor("elm") }
                            .also { it.isForcedToUseIdeaFileChooser = true })
            { update() }

    private val elmFormatPathField =
            fileSystemPathTextField(this, "Select 'elm-format'",
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                            .withFileFilter { it.name in ElmSuggest.executableNamesFor("elm-format") }
                            .also { it.isForcedToUseIdeaFileChooser = true })
            { update() }

    private val elmVersionLabel = JLabel()
    private val elmFormatVersionLabel = JLabel()
    private val elmFormatOnSaveCheckbox = JCheckBox()
    private val elmFormatShortcutLabel = HyperlinkLabel()

    override fun createComponent(): JComponent {
        elmFormatOnSaveCheckbox.addChangeListener { update() }
        elmFormatShortcutLabel.addHyperlinkListener {
            showActionShortcut(ElmExternalFormatAction.ID)
        }

        val panel = JPanel(VerticalFlowLayout()).apply {
            add(panel(title = "Elm Compiler") {
                row("Location:") {
                    cell {
                        elmPathField(CCFlags.growX)
                        button("Auto Discover", CCFlags.pushX) { elmPathField.text = autoDiscoverPathTo("elm") }
                    }
                }
                row("Version:") { elmVersionLabel() }
            })
            add(panel(title = "elm-format") {
                row("Location:") {
                    cell {
                        elmFormatPathField(CCFlags.growX)
                        button("Auto Discover", CCFlags.pushX) { elmFormatPathField.text = autoDiscoverPathTo("elm-format") }
                    }
                }
                row("Version:") { elmFormatVersionLabel() }
                row("Keyboard shortcut:") { elmFormatShortcutLabel() }
                row("Run when file saved?") { elmFormatOnSaveCheckbox() }
            })
        }

        // Whenever this panel appears, refresh just in case the user made changes on the Keymap settings screen.
        UiNotifyConnector(panel, object : Activatable.Adapter() {
            override fun showNotify() = update()
        }).also { Disposer.register(this, it) }

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

    private fun update() {
        val elmCompilerPath = Paths.get(elmPathField.text)
        val elmFormatPath = Paths.get(elmFormatPathField.text)
        val elmCLI = ElmCLI(elmCompilerPath)
        val elmFormatCLI = ElmFormatCLI(elmFormatPath)
        uiDebouncer.run(
                onPooledThread = { Pair(elmCLI.queryVersion(), elmFormatCLI.queryVersion()) },
                onUiThread = { (compilerResult, elmFormatResult) ->
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
                                    !elmCompilerPath.isValidFor("elm") -> {
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
                                    !elmFormatPath.isValidFor("elm-format") -> {
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

        elmPathField.text = elmCompilerPath
        elmFormatPathField.text = elmFormatPath
        elmFormatOnSaveCheckbox.isSelected = isElmFormatOnSaveEnabled

        update()
    }

    override fun apply() {
        project.elmWorkspace.modifySettings {
            it.copy(elmCompilerPath = elmPathField.text,
                    elmFormatPath = elmFormatPathField.text,
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
                || isOnSaveHookEnabledAndSelected() != settings.isElmFormatOnSaveEnabled
    }

    override fun getDisplayName() = "Elm"

    override fun getHelpTopic() = null
}

private fun Path.isValidFor(programName: String) =
        fileName != null && fileName.toString() in ElmSuggest.executableNamesFor(programName)