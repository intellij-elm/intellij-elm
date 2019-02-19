package org.elm.workspace.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
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
import org.elm.openapiext.pathToDirectoryTextField
import org.elm.workspace.ElmToolchain
import org.elm.workspace.elmWorkspace
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ElmWorkspaceConfigurable(
        private val project: Project
) : Configurable, Configurable.NoScroll, Disposable {

    private val versionUpdateDebouncer = UiDebouncer(this)

    private val pathToToolchainField = pathToDirectoryTextField(this,
            "Select directory containing the 'elm' binary") { update() }

    private val elmVersionLabel = JLabel()
    private val elmFormatLabel = JLabel()
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
                row("Directory containing 'elm' binary:") { pathToToolchainField(CCFlags.pushX) }
                row("Elm version:") { elmVersionLabel() }
            })
            add(panel(title = "elm-format") {
                row("Location:") { elmFormatLabel() }
                row("Version:") { elmFormatVersionLabel() }
                row("Keyboard shortcut:") { elmFormatShortcutLabel() }
                row("Run automatically when file saved?") { elmFormatOnSaveCheckbox() }
            })
        }

        // Whenever this panel appears, refresh just in case the user made changes on the Keymap settings screen.
        UiNotifyConnector(panel, object : Activatable.Adapter() {
            override fun showNotify() = update()
        }).also { Disposer.register(this, it) }

        return panel
    }

    private fun showActionShortcut(actionId: String) {
        val dataContext = DataManager.getInstance().getDataContext(elmFormatShortcutLabel)
        val allSettings = Settings.KEY.getData(dataContext) ?: return
        val keymapPanel = allSettings.find(KeymapPanel::class.java) ?: return
        allSettings.select(keymapPanel).doWhenDone {
            keymapPanel.selectAction(actionId)
        }
    }

    private fun update() {
        val pathToToolchain = pathToToolchainField.text
        val activateOnSaveHook = isOnSaveHookEnabledAndSelected()
        val elmToolchain = ElmToolchain(pathToToolchain, activateOnSaveHook)

        versionUpdateDebouncer.run(
                onPooledThread = {
                    Pair(
                            elmToolchain.queryCompilerVersion(),
                            elmToolchain.queryElmFormatVersion()
                    )
                },
                onUiThread = { versions ->
                    val (elmVersionResult, elmFormatVersionResult) = versions

                    when (elmVersionResult) {
                        is Result.Ok ->
                            when {
                                elmVersionResult.value < ElmToolchain.MIN_SUPPORTED_COMPILER_VERSION -> {
                                    elmVersionLabel.text = "${elmVersionResult.value} (not supported)"
                                    elmVersionLabel.foreground = JBColor.RED
                                }
                                else -> {
                                    elmVersionLabel.text = elmVersionResult.value.toString()
                                    elmVersionLabel.foreground = JBColor.foreground()
                                }
                            }
                        is Result.Err -> {
                            elmVersionLabel.text = "error: ${elmVersionResult.reason}"
                            elmVersionLabel.foreground = JBColor.RED
                        }
                    }

                    when (elmFormatVersionResult) {
                        is Result.Ok -> {
                            elmFormatVersionLabel.text = elmFormatVersionResult.value.toString()
                            elmFormatVersionLabel.foreground = JBColor.foreground()
                        }

                        is Result.Err -> {
                            elmFormatVersionLabel.text = "error: ${elmFormatVersionResult.reason}"
                            elmFormatVersionLabel.foreground = JBColor.RED
                        }
                    }
                }
        )
        when (val fmt = elmToolchain.elmFormat) {
            null -> {
                elmFormatLabel.text = "not found"
                elmFormatLabel.foreground = JBColor.RED
                elmFormatOnSaveCheckbox.isEnabled = false
                elmFormatShortcutLabel.isEnabled = false
            }
            else -> {
                elmFormatLabel.text = fmt.elmFormatExecutablePath.toString()
                elmFormatLabel.foreground = JBColor.foreground()
                elmFormatOnSaveCheckbox.isEnabled = true
                elmFormatShortcutLabel.isEnabled = true
            }
        }

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
        val binDirPath = settings.binDirPath ?: ElmToolchain.suggest(project)?.binDirPath
        val isElmFormatOnSaveEnabled = settings.isElmFormatOnSaveEnabled

        pathToToolchainField.text = binDirPath?.toString() ?: ""
        elmFormatOnSaveCheckbox.isSelected = isElmFormatOnSaveEnabled

        update()
    }

    override fun apply() {
        project.elmWorkspace.modifySettings {
            it.copy(binDirPath = pathToToolchainField.text,
                    isElmFormatOnSaveEnabled = isOnSaveHookEnabledAndSelected()
            )
        }
    }

    private fun isOnSaveHookEnabledAndSelected() =
            elmFormatOnSaveCheckbox.isEnabled && elmFormatOnSaveCheckbox.isSelected

    override fun isModified(): Boolean {
        val settings = project.elmWorkspace.rawSettings
        return pathToToolchainField.text != settings.binDirPath
                || isOnSaveHookEnabledAndSelected() != settings.isElmFormatOnSaveEnabled
    }

    override fun getDisplayName() = "Elm"

    override fun getHelpTopic() = null
}
