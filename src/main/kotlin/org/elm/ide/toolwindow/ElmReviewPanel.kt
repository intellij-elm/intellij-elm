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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import org.elm.ide.actions.ELM_REVIEW_ACTION_ID
import org.elm.ide.actions.ElmExternalReviewAction
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByPathTestAware
import org.elm.openapiext.toPsiFile
import org.elm.workspace.elmreview.ElmReviewError
import org.elm.workspace.elmreview.Region
import org.elm.workspace.elmreview.Start
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.font.TextAttribute
import java.nio.file.Path
import javax.swing.*
import javax.swing.ScrollPaneConstants.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class ElmReviewPanel(
        private val project: Project,
        private val contentManager: ContentManager
) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    private var baseDirPath: Path? = null
    private var selectedReviewMessage: Int = 0

    var reviewErrors: List<ElmReviewError> = emptyList()
        set(value) {
            checkIsEventDispatchThread()
            field = value
            selectedReviewMessage = 0

            when {
                reviewErrors.isEmpty() -> {
                    tableUI.model = emptyErrorTable
                    detailsUI.text = """<p style="color:white;margin-left: 10px">No Errors</p>"""
                }
                else -> {
                    val cellValues = reviewErrors.map {
                        arrayOf(it.region.pretty(),
                            it.rule,
                            it.message)
                    }.toTypedArray()
                    tableUI.model = object : DefaultTableModel(cellValues, errorTableColumnNames) {
                        override fun isCellEditable(row: Int, column: Int) = false
                    }
                    tableUI.setRowSelectionInterval(0, 0)

                    detailsUI.text = reviewErrors[0].html
                }
            }
        }

    private fun Region.pretty() = "${start.line} : ${start.column}"

    // TOOLWINDOW TOOLBAR

    private fun createToolbar(): JComponent {
        val reviewPanel = this
        val toolbar = with(ActionManager.getInstance()) {
            val buttonGroup = DefaultActionGroup().apply {
                add(getAction(ELM_REVIEW_ACTION_ID))
                addSeparator()
                add(CommonActionsManager.getInstance().createNextOccurenceAction(reviewPanel))
                add(CommonActionsManager.getInstance().createPrevOccurenceAction(reviewPanel))
            }
            createActionToolbar("Elm Review Toolbar", buttonGroup, true)
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
                if (!it.valueIsAdjusting && reviewErrors.isNotEmpty() && selectedRow >= 0) {
                    val cellRect = getCellRect(selectedRow, 0, true)
                    scrollRectToVisible(cellRect)
                    selectedReviewMessage = selectedRow
                    detailsUI.text = reviewErrors[selectedReviewMessage].html
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
    private val splitterUI =
        JBSplitter("ElmReviewErrorPanel", 0.4F).apply {
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
                add(splitterUI, BorderLayout.CENTER)
            }

    // INIT

    init {
        toolbar = createToolbar()
        setContent(contentUI)

        with(project.messageBus.connect()) {
            subscribe(ElmExternalReviewAction.ERRORS_TOPIC, object : ElmExternalReviewAction.ElmReviewErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmReviewError>, targetPath: String?, offset: Int) {
                    this@ElmReviewPanel.baseDirPath = baseDirPath

                    reviewErrors = messages
                    selectedReviewMessage = 0
                    tableUI.setRowSelectionInterval(0, 0)

                    val numErrors = reviewErrors.size
                    contentManager.getContent(0)?.displayName = "$numErrors ${pluralize("error", numErrors)}"
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
        val elmReviewError = reviewErrors.getOrNull(selectedReviewMessage) ?: return null
        val virtualFile = baseDirPath?.resolve(elmReviewError.path)?.let { findFileByPathTestAware(it) } ?: return null
        val psiFile = virtualFile.toPsiFile(project) ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: return null
        val start = elmReviewError.region.start
        return Triple(virtualFile, document, start)
    }

    override fun dispose() {}

    // OCCURRENCE NAVIGATOR
    private fun calcNextOccurrence(direction: OccurenceDirection, go: Boolean = false): OccurenceInfo? {
        if (reviewErrors.isEmpty()) return null

        val nextIndex = when (direction) {
            is OccurenceDirection.Forward -> selectedReviewMessage + 1
            is OccurenceDirection.Back -> selectedReviewMessage - 1
        }

        val elmError = reviewErrors.getOrNull(nextIndex) ?: return null

        if (go) {
            // update selection
            selectedReviewMessage = nextIndex
            detailsUI.text = elmError.html
            tableUI.setRowSelectionInterval(selectedReviewMessage, selectedReviewMessage)
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

        val errorTableColumnNames = arrayOf("Line : Column", "Rule", "Message")

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
