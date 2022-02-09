package org.elm.ide.toolwindow

import com.intellij.ide.CommonActionsManager
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.OccurenceNavigator.OccurenceInfo
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.text.StringUtil.pluralize
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByPath
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.*
import java.awt.*
import java.awt.font.TextAttribute
import java.nio.file.Path
import javax.swing.*
import javax.swing.ScrollPaneConstants.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class ElmCompilerPanel(
        private val project: Project,
        private val contentManager: ContentManager
) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    private var baseDirPath: Path? = null
    private var selectedCompilerMessage: Int = 0

    var compilerMessages: List<ElmError> = emptyList()
        set(value) {
            checkIsEventDispatchThread()
            field = value
            selectedCompilerMessage = 0

            // update UI
            when {
                compilerMessages.isEmpty() -> {
                    tableUI.model = emptyErrorTable
                    detailsUI.text = """<p style="color:white;margin-left: 10px">No Errors</p>"""
                }
                else -> {
                    val cellValues = compilerMessages.map {
                        arrayOf(it.location?.moduleName ?: "n/a",
                                it.location?.region?.pretty() ?: "n/a",
                                toNiceName(it.title))
                    }.toTypedArray()
                    tableUI.model = object : DefaultTableModel(cellValues, errorTableColumnNames) {
                        override fun isCellEditable(row: Int, column: Int) = false
                    }
                    tableUI.setRowSelectionInterval(0, 0)

                    detailsUI.text = compilerMessages[0].html
                }
            }
        }

    private fun toNiceName(title: String) =
            title.split(" ").joinToString(" ") { it.first() + it.substring(1).toLowerCase() }

    private fun Region.pretty() = "${start.line} : ${start.column}"

    // TOOLWINDOW TOOLBAR

    private fun createToolbar(): JComponent {
        val compilerPanel = this
        val toolbar = with(ActionManager.getInstance()) {
            val buttonGroup = DefaultActionGroup().apply {
                add(getAction(ELM_BUILD_ACTION_ID))
                addSeparator()
                add(CommonActionsManager.getInstance().createNextOccurenceAction(compilerPanel))
                add(CommonActionsManager.getInstance().createPrevOccurenceAction(compilerPanel))
            }
            createActionToolbar("Elm Compiler Toolbar", buttonGroup, true)
        }
        toolbar.setTargetComponent(this)
        return toolbar.component
    }

    // LEFT PANEL

    private val tableUI = JBTable().apply {
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
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        tableHeader.defaultRenderer = errorTableHeaderRenderer
        setDefaultRenderer(Any::class.java, errorTableCellRenderer)
        selectionModel.addListSelectionListener { event ->
            event.let {
                if (!it.valueIsAdjusting && compilerMessages.isNotEmpty() && selectedRow >= 0) {
                    val cellRect = getCellRect(selectedRow, 0, true)
                    scrollRectToVisible(cellRect)
                    selectedCompilerMessage = selectedRow
                    detailsUI.text = compilerMessages[selectedCompilerMessage].html
                }
            }
        }
    }

    // RIGHT PANEL

    private val detailsUI = JTextPane().apply {
        contentType = "text/html"
        isEditable = false
        background = backgroundColorUI
        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    // TOOLWINDOW CONTENT

    private val entryPointLabel = JLabel("No project compiled yet")

    // TODO 2 entryPoints Lamdera
    private val entryPointLink =
            LinkLabel<String>("", null)

    private val entryPointUI =
            JPanel(FlowLayout(FlowLayout.LEADING)).apply {
                add(entryPointLabel)
                add(entryPointLink)
            }


    private val splitterUI =
            JBSplitter("ElmCompilerErrorPanel", 0.4F).apply {
                firstComponent =
                        JBScrollPane(
                                tableUI,
                                VERTICAL_SCROLLBAR_AS_NEEDED,
                                HORIZONTAL_SCROLLBAR_NEVER
                        )
                secondComponent =
                        JBScrollPane(
                                detailsUI,
                                VERTICAL_SCROLLBAR_AS_NEEDED,
                                HORIZONTAL_SCROLLBAR_AS_NEEDED
                        )
            }

    private val contentUI =
            JPanel(BorderLayout())
                    .apply {
                        add(entryPointUI, BorderLayout.NORTH)
                        add(splitterUI, BorderLayout.CENTER)
                    }

    // INIT

    init {
        setToolbar(createToolbar())
        setContent(contentUI)

        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String, offset: Int) {
                    this@ElmCompilerPanel.baseDirPath = baseDirPath

                    compilerMessages = messages
                    selectedCompilerMessage = 0
                    tableUI.setRowSelectionInterval(0, 0)

                    val numErrors = compilerMessages.size
                    contentManager.getContent(0)?.displayName = "$numErrors ${pluralize("error", numErrors)}"

                    entryPointLabel.text = "$baseDirPath: elm make"
                    entryPointLink.text = targetPath
                    entryPointLink.setListener({ _, _ ->
                        VfsUtil.findFile(baseDirPath.resolve(targetPath), true)
                                ?.let {
                                    OpenFileDescriptor(project, it, offset).navigate(true)
                                }
                    }, null)
                }
            })
        }
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
        val elmError = compilerMessages.getOrNull(selectedCompilerMessage) ?: return null
        val elmLocation = elmError.location ?: return null
        val virtualFile = baseDirPath?.resolve(elmLocation.path)?.let {
            LocalFileSystem.getInstance().findFileByPath(it)
        } ?: return null
        val psiFile = virtualFile.toPsiFile(project) ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: return null
        val start = elmLocation.region?.start ?: return null
        return Triple(virtualFile, document, start)
    }

    override fun dispose() {}

    // OCCURRENCE NAVIGATOR
    private fun calcNextOccurrence(direction: OccurenceDirection, go: Boolean = false): OccurenceInfo? {
        if (compilerMessages.isEmpty()) return null

        val nextIndex = when (direction) {
            is OccurenceDirection.Forward -> selectedCompilerMessage + 1
            is OccurenceDirection.Back -> selectedCompilerMessage - 1
        }

        val elmError = compilerMessages.getOrNull(nextIndex) ?: return null

        if (go) {
            // update selection
            selectedCompilerMessage = nextIndex
            detailsUI.text = elmError.html
            tableUI.setRowSelectionInterval(selectedCompilerMessage, selectedCompilerMessage)
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

    private companion object {

        sealed class OccurenceDirection {
            object Forward : OccurenceDirection()
            object Back : OccurenceDirection()
        }

        val backgroundColorUI = Color(0x23, 0x31, 0x42)

        val emptyErrorTable = DefaultTableModel(arrayOf<Array<String>>(emptyArray()), emptyArray())

        val errorTableColumnNames = arrayOf("Module", "Line : Column", "Type")

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
    }
}
