package org.elm.ide.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.HorizontalLayout
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByPath
import org.elm.utils.CircularList
import org.elm.workspace.compiler.*
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JTextPane
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants


class ElmCompilerPanel(private val project: Project) : SimpleToolWindowPanel(true, false) {

    // TODO for errorListUI: F4 to source + sync messageUI

    private val errorListUI = JBList<String>(emptyList()).apply {
        emptyText.text = ""
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        object : AutoScrollToSourceHandler() {
            override fun isAutoScrollMode() = true
            override fun setAutoScrollMode(state: Boolean) {}
        }.install(this)
    }

    private val messageUI = JTextPane().apply {
        contentType = "text/html"
        // background = Color(200, 200, 200) // TODO color themes
    }

    private var compilerMessages: CircularList<CompilerMessage> = CircularList(emptyList())
        set(value) {
            checkIsEventDispatchThread()
            field = value
            if (compilerMessages.isEmpty()) {
                errorListUI.setListData(emptyArray())
                messageUI.text = "No Errors"
            } else {
                val titles = compilerMessages.list.map { it.path + prettyRegion(it.messageWithRegion.region) + "    " + it.messageWithRegion.title }
                errorListUI.setListData(titles.toTypedArray())
                messageUI.text = compilerMessages.get().messageWithRegion.message
            }
        }

    private fun prettyRegion(region: Region): String {
        return ": line ${region.start.line} column ${region.start.column}"
    }

    init {
        setToolbar(createToolbar())

        val jbPanel = JBPanel<JBPanel<*>>(HorizontalLayout(3, SwingConstants.TOP))
        jbPanel.add(errorListUI)
        jbPanel.add(messageUI)
        setContent(jbPanel)

        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(messages: List<CompilerMessage>) {
                    ApplicationManager.getApplication().invokeLater {
                        compilerMessages = CircularList(messages)
                    }
                }
            })
            subscribe(ElmForwardAction.ERRORS_FORWARD_TOPIC, object : ElmForwardAction.ElmErrorsForwardListener {
                override fun forward() {
                    if (!compilerMessages.isEmpty())
                        ApplicationManager.getApplication().invokeLater {
                            messageUI.text = compilerMessages.next().messageWithRegion.message
                        }
                }
            })
            subscribe(ElmBackAction.ERRORS_BACK_TOPIC, object : ElmBackAction.ElmErrorsBackListener {
                override fun back() {
                    if (!compilerMessages.isEmpty())
                        ApplicationManager.getApplication().invokeLater {
                            messageUI.text = compilerMessages.prev().messageWithRegion.message
                        }
                }
            })
        }
    }


    private fun createToolbar(): JComponent {
        val toolbar = with(ActionManager.getInstance()) {
            createActionToolbar(
                    "Elm Compiler Toolbar", // the value here doesn't matter, as far as I can tell
                    getAction("Elm.CompilerToolsGroup") as DefaultActionGroup, // defined in plugin.xml
                    true // horizontal layout
            )
        }
        toolbar.setTargetComponent(this)
        return toolbar.component
    }

    // todo: F4 to source calls twice here ?
    override fun getData(dataId: String): Any? {
        return when {
            CommonDataKeys.NAVIGATABLE.`is`(dataId) -> {
                val file = LocalFileSystem.getInstance().findFileByPath(Paths.get(project.basePath + "/" + compilerMessages.get().path))
                val start = compilerMessages.get().messageWithRegion.region.start
                OpenFileDescriptor(project, file!!, start.line - 1, start.column - 1)
            }
            else ->
                super.getData(dataId)
        }
    }
}
