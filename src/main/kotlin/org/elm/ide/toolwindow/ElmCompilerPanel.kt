package org.elm.ide.toolwindow

import com.intellij.ide.CommonActionsManager
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.CompilerMessage
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.Region
import org.elm.workspace.compiler.Start
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.font.TextAttribute
import java.util.*
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTextPane
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class ElmCompilerPanel(private val project: Project, private val contentManager: ContentManager) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    private fun updateSelectionAndCreateOccurenceInfo(): OccurenceNavigator.OccurenceInfo {
        // update selection
        messageUI.text = compilerMessages[indexCompilerMessages].messageWithRegion.message
        errorTableUI.setRowSelectionInterval(indexCompilerMessages, indexCompilerMessages)

        // create occurence info
        val (virtualFile, document, start) = startFromErrorMessage(project.guessProjectDir())
        document?.let {
            val offset = it.getLineStartOffset(start.line - 1) + start.column - 1
            return OccurenceNavigator.OccurenceInfo(PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, offset), -1, -1)
        }
        throw RuntimeException("The impossible happened...")
    }

    override fun getNextOccurenceActionName(): String {
        return "Next Error"
    }

    override fun hasNextOccurence(): Boolean {
        return !compilerMessages.isEmpty() && indexCompilerMessages < compilerMessages.lastIndex
    }

    override fun goNextOccurence(): OccurenceNavigator.OccurenceInfo {
        if (compilerMessages.isNotEmpty()) {
            if (indexCompilerMessages < compilerMessages.lastIndex) {
                indexCompilerMessages += 1
            }
            return updateSelectionAndCreateOccurenceInfo()
        }
        return OccurenceNavigator.OccurenceInfo.position(0, 0)
    }

    override fun getPreviousOccurenceActionName(): String {
        return "Previous Error"
    }

    override fun hasPreviousOccurence(): Boolean {
        return !compilerMessages.isEmpty() && indexCompilerMessages > 0
    }

    override fun goPreviousOccurence(): OccurenceNavigator.OccurenceInfo {
        if (compilerMessages.isNotEmpty()) {
            if (indexCompilerMessages > 0) {
                indexCompilerMessages -= 1
            }
            return updateSelectionAndCreateOccurenceInfo()
        }
        return OccurenceNavigator.OccurenceInfo.position(0, 0)
    }

    override fun dispose() {
        with(ActionManager.getInstance()) {
            val defaultActionGroup = getAction("Elm.CompilerToolsGroup") as DefaultActionGroup
            defaultActionGroup.remove(nextOccurenceAction)
            defaultActionGroup.remove(prevOccurenceAction)
        }
    }

    private val backgroundColorUI = Color(0x23, 0x31, 0x42)

    private val emptyErrorTable = DefaultTableModel(arrayOf<Array<String>>(arrayOf()), arrayOf())

    private val errorTableUI = JBTable().apply {
        setShowGrid(false)
        intercellSpacing = Dimension(2, 2)
        border = emptyBorder
        background = backgroundColorUI
        emptyText.text = ""
        model = emptyErrorTable
        object : AutoScrollToSourceHandler() {
            override fun isAutoScrollMode() = true
            override fun setAutoScrollMode(state: Boolean) {}
        }.install(this)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
        selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && selectedRow >= 0) {
                val cellRect = getCellRect(selectedRow, 0, true)
                scrollRectToVisible(cellRect)

                indexCompilerMessages = selectedRow
                messageUI.text = compilerMessages[indexCompilerMessages].messageWithRegion.message
            }
        }
    }

    private val messageUI = JTextPane().apply {
        contentType = "text/html"
        background = backgroundColorUI
    }

    private var indexCompilerMessages: Int = 0

    private val noErrorContent = JBLabel()

    var compilerMessages: List<CompilerMessage> = emptyList()
        set(value) {
            checkIsEventDispatchThread()
            field = value
            if (compilerMessages.isEmpty()) {
                setContent(noErrorContent)

                errorTableUI.model = emptyErrorTable
                messageUI.text = ""
            } else {
                setContent(errorContent)

                indexCompilerMessages = 0
                messageUI.text = compilerMessages[0].messageWithRegion.message

                val locationsAndType: Array<Array<String>> = compilerMessages.map {
                    arrayOf(it.name, prettyRegion(it.messageWithRegion.region), toNiceName(it.messageWithRegion.title))
                }.toTypedArray()
                errorTableUI.model = DefaultTableModel(locationsAndType, arrayOf("Module", "Location", "Type"))
                errorTableUI.tableHeader.defaultRenderer = errorTableHeaderRenderer
                errorTableUI.setDefaultRenderer(errorTableUI.getColumnClass(0), errorTableCellRenderer)
                errorTableUI.setDefaultRenderer(errorTableUI.getColumnClass(1), errorTableCellRenderer)
                errorTableUI.setDefaultRenderer(errorTableUI.getColumnClass(2), errorTableCellRenderer)
            }
        }

    private fun toNiceName(title: String): String {
        return title.split(" ").joinToString(" ") { it.first() + it.substring(1).toLowerCase() }
    }

    private fun prettyRegion(region: Region): String {
        return " line ${region.start.line} column ${region.start.column}"
    }

    private var errorContent: JBSplitter

    private val emptyBorder = EmptyBorder(10, 10, 10, 10)

    init {
        setToolbar(createToolbar())

        errorContent = JBSplitter("ElmCompilerErrorPanel", 0.4F)
        errorContent.firstComponent = JBScrollPane(errorTableUI, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        errorContent.firstComponent.border = emptyBorder
        errorContent.secondComponent = messageUI
        errorContent.secondComponent.border = emptyBorder
        setContent(noErrorContent)

        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(messages: List<CompilerMessage>) {
                    ApplicationManager.getApplication().invokeLater {
                        compilerMessages = messages
                        contentManager.getContent(0)?.displayName = "${compilerMessages.size} errors"
                        errorTableUI.setRowSelectionInterval(0, 0)
                        indexCompilerMessages = 0
                    }
                }
            })
        }
    }

    private lateinit var nextOccurenceAction: AnAction

    private lateinit var prevOccurenceAction: AnAction

    private fun createToolbar(): JComponent {
        val compilerPanel = this
        val toolbar = with(ActionManager.getInstance()) {
            val defaultActionGroup = getAction("Elm.CompilerToolsGroup") as DefaultActionGroup

            nextOccurenceAction = CommonActionsManager.getInstance().createNextOccurenceAction(compilerPanel)
            prevOccurenceAction = CommonActionsManager.getInstance().createPrevOccurenceAction(compilerPanel)
            defaultActionGroup.addSeparator()
            defaultActionGroup.add(nextOccurenceAction)
            defaultActionGroup.add(prevOccurenceAction)

            createActionToolbar(
                    "Elm Compiler Toolbar", // the value here doesn't matter, as far as I can tell
                    getAction("Elm.CompilerToolsGroup") as DefaultActionGroup, // defined in plugin.xml
                    true // horizontal layout
            )
        }
        toolbar.setTargetComponent(this)
        return toolbar.component
    }

    override fun getData(dataId: String): Any? {
        return when {
            CommonDataKeys.NAVIGATABLE.`is`(dataId) -> {
                if (!compilerMessages.isEmpty()) {
                    val (virtualFile, _, start) = startFromErrorMessage(project.guessProjectDir())
                    return OpenFileDescriptor(project, virtualFile, start.line - 1, start.column - 1)
                } else {
                    null
                }
            }
            else ->
                super.getData(dataId)
        }
    }

    private fun startFromErrorMessage(baseDir: VirtualFile?): Triple<VirtualFile, Document?, Start> {
        val path = compilerMessages[indexCompilerMessages].path
        val virtualFile =
                if (FileUtil.isAbsolute(path))
                    LocalFileSystem.getInstance().findFileByPath(path)
                else {
                    VfsUtil.findRelativeFile(path, baseDir)
                }
        val psiFile = virtualFile?.toPsiFile(project)
        psiFile?.let {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            val start = compilerMessages[indexCompilerMessages].messageWithRegion.region.start
            return Triple(virtualFile, document, start)
        }
        throw RuntimeException("The impossible happened... virtualFile is null for '$path'")
    }

    companion object {

        val errorTableHeaderRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                rendererComponent.foreground = Color.WHITE
                return rendererComponent
            }
        }

        val errorTableCellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                rendererComponent.foreground = Color.LIGHT_GRAY
                border = EmptyBorder(2, 2, 2, 2)
                if (column == 2) {
                    rendererComponent.font = font.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD))
                }
                return rendererComponent
            }
        }
    }

}
