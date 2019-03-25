package org.elm.ide.toolwindow

import com.intellij.ide.CommonActionsManager
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.OccurenceNavigator.OccurenceInfo
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.ActionLink
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByPathTestAware
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.ElmError
import org.elm.workspace.compiler.Region
import org.elm.workspace.compiler.Start
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.font.TextAttribute
import java.nio.file.Path
import javax.swing.*
import javax.swing.ScrollPaneConstants.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class ElmCompilerPanel(
        private val project: Project,
        private val contentManager: ContentManager
) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    var baseDirPath: Path? = null

    override fun dispose() {}

    private var indexCompilerMessages: Int = 0

    var compilerMessages: List<ElmError> = emptyList()
        set(value) {
            checkIsEventDispatchThread()
            field = value
            indexCompilerMessages = 0

            // update UI
            if (compilerMessages.isEmpty()) {
                setContent(emptyUI)
                errorTableUI.model = emptyErrorTable
                messageUI.text = ""
            } else {
                setContent(errorUI)
                messageUI.text = compilerMessages[0].html
                val cellValues = compilerMessages.map {
                    arrayOf(it.location?.moduleName ?: "n/a",
                            it.location?.region?.pretty() ?: "n/a",
                            toNiceName(it.title))
                }.toTypedArray()
                errorTableUI.model = object : DefaultTableModel(cellValues, errorTableColumnNames) {
                    override fun isCellEditable(row: Int, column: Int) = false
                }
                errorTableUI.setRowSelectionInterval(0, 0)
            }
        }

    private val errorTableSelectionListener = ListSelectionListener { event ->
        event.let {
            if (!it.valueIsAdjusting && errorTableUI.selectedRow >= 0) {
                val cellRect = errorTableUI.getCellRect(errorTableUI.selectedRow, 0, true)
                scrollRectToVisible(cellRect)
                if (compilerMessages.isNotEmpty()) {
                    indexCompilerMessages = errorTableUI.selectedRow
                    messageUI.text = compilerMessages[indexCompilerMessages].html
                }
            }
        }
    }

    private val errorTableUI = JBTable().apply {
        setShowGrid(false)
        intercellSpacing = Dimension(2, 2)
        border = EmptyBorder(3, 3, 3, 3)
        background = backgroundColorUI
        selectionBackground = Color(0x11, 0x51, 0x73)
        emptyText.text = ""
        model = emptyErrorTable
        object : AutoScrollToSourceHandler() {
            override fun isAutoScrollMode() = true
            override fun setAutoScrollMode(state: Boolean) {}
        }.install(this)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
        tableHeader.defaultRenderer = errorTableHeaderRenderer
        setDefaultRenderer(Any::class.java, errorTableCellRenderer)
    }

    // RIGHT PANEL
    private val messageUI = JTextPane().apply {
        contentType = "text/html"
        isEditable = false
        background = backgroundColorUI
        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    private val errorUI = JBSplitter("ElmCompilerErrorPanel", 0.4F).apply {
        firstComponent = JPanel(BorderLayout()).apply {
            add(JBLabel()) // dummy-placeholder component at index 0 (gets replaced by org.elm.workspace.compiler.ElmBuildAction.ElmErrorsListener.update)
            add(JBScrollPane(errorTableUI, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER)
        }
        secondComponent = JBScrollPane(messageUI, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED)
    }

    private fun toNiceName(title: String) =
            title.split(" ").joinToString(" ") { it.first() + it.substring(1).toLowerCase() }

    private fun Region.pretty() = "line ${start.line}, column ${start.column}"

    init {
        setToolbar(createToolbar())
        setContent(emptyUI)

        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String?) {
                    this@ElmCompilerPanel.baseDirPath = baseDirPath

                    errorTableUI.selectionModel.removeListSelectionListener(errorTableSelectionListener)
                    compilerMessages = messages
                    indexCompilerMessages = 0
                    errorTableUI.setRowSelectionInterval(0, 0)
                    errorTableUI.selectionModel.addListSelectionListener(errorTableSelectionListener)

                    contentManager.getContent(0)?.displayName = "${compilerMessages.size} errors"

                    val compilerTargetUI = createCompilerTargetUI(baseDirPath, targetPath)
                    errorUI.firstComponent.remove(0)
                    errorUI.firstComponent.add(compilerTargetUI, BorderLayout.NORTH, 0)
                }
            })
        }
    }

    private fun createCompilerTargetUI(baseDirPath: Path, targetPath: String?): ActionLink {
        return ActionLink("", object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                e.project?.let {
                    val targetFile = VfsUtil.findFile(baseDirPath.resolve(targetPath), true) ?: return
                    val descriptor = OpenFileDescriptor(it, targetFile, 0, 0)
                    descriptor.navigate(true)
                }
            }
        }).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            setNormalColor(Color.BLACK)
            activeColor = Color.BLACK
            text = "Compiler Target  $targetPath"
        }
    }

    private fun createToolbar(): JComponent {
        val compilerPanel = this
        val toolbar = with(ActionManager.getInstance()) {
            val buttonGroup = DefaultActionGroup().apply {
                add(getAction("Elm.Build"))
                addSeparator()
                add(CommonActionsManager.getInstance().createNextOccurenceAction(compilerPanel))
                add(CommonActionsManager.getInstance().createPrevOccurenceAction(compilerPanel))
            }
            createActionToolbar("Elm Compiler Toolbar", buttonGroup, true)
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


    // OCCURRENCE NAVIGATOR

    sealed class OccurenceDirection {
        object Forward : OccurenceDirection()
        object Back : OccurenceDirection()
    }

    private fun calcNextOccurrence(direction: OccurenceDirection, go: Boolean = false): OccurenceInfo? {
        if (compilerMessages.isEmpty()) return null

        val nextIndex = when(direction) {
            is OccurenceDirection.Forward -> if (indexCompilerMessages < compilerMessages.lastIndex)
                                                indexCompilerMessages + 1
                                                else return null
            is OccurenceDirection.Back    -> if (indexCompilerMessages > 0)
                                                indexCompilerMessages - 1
                                                else return null
        }

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
    override fun hasNextOccurence() = calcNextOccurrence(OccurenceDirection.Forward) != null
    override fun goNextOccurence(): OccurenceInfo? = calcNextOccurrence(OccurenceDirection.Forward, go = true)

    override fun getPreviousOccurenceActionName() = "Previous Error"
    override fun hasPreviousOccurence() = calcNextOccurrence(OccurenceDirection.Back) != null
    override fun goPreviousOccurence(): OccurenceInfo? = calcNextOccurrence(OccurenceDirection.Back, go = true)

    // UI COMPONENTS
    private companion object {

        val backgroundColorUI = Color(0x23, 0x31, 0x42)

        val emptyErrorTable = DefaultTableModel(arrayOf<Array<String>>(arrayOf()), arrayOf())

        val errorTableColumnNames = arrayOf("Module", "Location", "Type")

        val errorTableHeaderRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                            .apply { foreground = Color.WHITE }
        }
        val errorTableCellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                border = EmptyBorder(2, 2, 2, 2)
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                        .apply {
                            foreground = Color.LIGHT_GRAY
                            if (column == 2) {
                                font = font.deriveFont(mapOf(TextAttribute.WEIGHT to TextAttribute.WEIGHT_BOLD))
                            }
                        }
            }
        }

        val emptyUI = JBPanelWithEmptyText()
    }
}
