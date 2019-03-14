package org.elm.ide.toolwindow

import com.intellij.ide.CommonActionsManager
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.OccurenceNavigator.OccurenceInfo
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
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByPathTestAware
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.ElmError
import org.elm.workspace.compiler.Region
import org.elm.workspace.compiler.Start
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.font.TextAttribute
import java.nio.file.Path
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
import kotlin.math.sign


class ElmCompilerPanel(private val project: Project, private val contentManager: ContentManager) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    var baseDirPath: Path? = null


    // OCCURRENCE NAVIGATOR


    private fun calcNextOccurrence(direction: Int, go: Boolean = false): OccurenceInfo? {
        if (compilerMessages.isEmpty()) return null

        val nextIndex = indexCompilerMessages + direction.sign
        val elmError = compilerMessages.getOrNull(nextIndex) ?: return null

        if (go) {
            // update selection
            indexCompilerMessages = nextIndex
            messageUI.text = elmError.html
            errorTableUI.setRowSelectionInterval(indexCompilerMessages, indexCompilerMessages)
        }

        // create occurrence info
        val (virtualFile, document, start) = startFromErrorMessage() ?: return null
        val offset = document.getLineStartOffset(start.line - 1) + start.column - 1
        val navigatable = PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, offset)
        return OccurenceInfo(navigatable, -1, -1)
    }

    override fun getNextOccurenceActionName() = "Next Error"
    override fun hasNextOccurence() = calcNextOccurrence(1) != null
    override fun goNextOccurence(): OccurenceInfo? = calcNextOccurrence(1, go = true)

    override fun getPreviousOccurenceActionName() = "Previous Error"
    override fun hasPreviousOccurence() = calcNextOccurrence(-1) != null
    override fun goPreviousOccurence(): OccurenceInfo? = calcNextOccurrence(-1, go = true)


    // UI

    override fun dispose() {
        // TODO [kl] why is this here?
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
        selectionBackground = Color(0x11, 0x51, 0x73)
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
                if (compilerMessages.isNotEmpty()) {
                    indexCompilerMessages = selectedRow
                    messageUI.text = compilerMessages[indexCompilerMessages].html
                }
            }
        }
    }

    private val messageUI = JTextPane().apply {
        contentType = "text/html"
        background = backgroundColorUI
    }

    private var indexCompilerMessages: Int = 0

    private val noErrorContent = JBLabel()

    var compilerMessages: List<ElmError> = emptyList()
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
                messageUI.text = compilerMessages[0].html

                val locationsAndType: Array<Array<String>> = compilerMessages.map {
                    arrayOf(it.location?.moduleName ?: "n/a",
                            it.location?.region?.pretty() ?: "n/a",
                            toNiceName(it.title))
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

    private fun Region.pretty(): String {
        return " line ${start.line}, column ${start.column}"
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
                override fun update(baseDirPath: Path, messages: List<ElmError>) {
                    ApplicationManager.getApplication().invokeLater {
                        this@ElmCompilerPanel.baseDirPath = baseDirPath
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
                val (virtualFile, _, start) = startFromErrorMessage() ?: return null
                return OpenFileDescriptor(project, virtualFile, start.line - 1, start.column - 1)
            }
            else ->
                super.getData(dataId)
        }
    }

    private fun startFromErrorMessage(): Triple<VirtualFile, Document, Start>? {
        val elmError = compilerMessages.getOrNull(indexCompilerMessages) ?: return null
        val elmLocation = elmError.location ?: return null
        val virtualFile = baseDirPath?.resolve(elmLocation.path)?.let { findFileByPathTestAware(it) } ?: return null
        val psiFile = virtualFile.toPsiFile(project) ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return null
        val start = elmLocation.region?.start ?: return null
        return Triple(virtualFile, document, start)
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
