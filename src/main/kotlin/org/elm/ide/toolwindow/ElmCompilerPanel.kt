package org.elm.ide.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.findFileByMaybeRelativePath
import org.elm.openapiext.findFileByPath
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.CompilerMessage
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.Region
import java.awt.Color
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextPane
import javax.swing.ListSelectionModel


class ElmCompilerPanel(private val project: Project, private val contentManager: ContentManager) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    private fun occurenceInfo(): OccurenceNavigator.OccurenceInfo {
        val filePath = compilerMessages[indexCompilerMessages].path
        val virtualFile = project.projectFile?.findFileByMaybeRelativePath(filePath)
        val psiFile = virtualFile?.toPsiFile(project)
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile!!)
        val start = compilerMessages[indexCompilerMessages].messageWithRegion.region.start
        val lineStartOffset = document!!.getLineStartOffset(start.line - 1)
        return OccurenceNavigator.OccurenceInfo(PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, lineStartOffset), -1, -1)
    }

    override fun getNextOccurenceActionName(): String {
        return "Next Error"
    }

    override fun hasNextOccurence(): Boolean {
        return !compilerMessages.isEmpty() && indexCompilerMessages < compilerMessages.lastIndex
    }

    override fun goNextOccurence(): OccurenceNavigator.OccurenceInfo {
        if (!compilerMessages.isEmpty()) {
            if (indexCompilerMessages < compilerMessages.lastIndex) {
                indexCompilerMessages += 1
            }
            messageUI.text = compilerMessages[indexCompilerMessages].messageWithRegion.message
            errorListUI.selectedIndex = indexCompilerMessages
            return occurenceInfo()
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
        if (!compilerMessages.isEmpty()) {
            if (indexCompilerMessages > 0) {
                indexCompilerMessages -= 1
            }
            messageUI.text = compilerMessages[indexCompilerMessages].messageWithRegion.message
            errorListUI.selectedIndex = indexCompilerMessages
            return occurenceInfo()
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

    private val errorListUI = JBList<String>(emptyList()).apply {
        emptyText.text = ""
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : ColoredListCellRenderer<String>() {
            override fun customizeCellRenderer(list: JList<out String>, value: String, index: Int, selected: Boolean, hasFocus: Boolean) {
                icon = AllIcons.General.Error
                append(value, REGULAR_ATTRIBUTES)
            }
        }
        object : AutoScrollToSourceHandler() {
            override fun isAutoScrollMode() = true
            override fun setAutoScrollMode(state: Boolean) {}
        }.install(this)
        addListSelectionListener {
            if (!it.valueIsAdjusting && selectedIndex >= 0) {
                indexCompilerMessages = selectedIndex
                messageUI.text = compilerMessages[indexCompilerMessages].messageWithRegion.message
            }
        }
    }

    private val messageUI = JTextPane().apply {
        contentType = "text/html"
        background = Color(34, 34, 34)
    }

    private var indexCompilerMessages: Int = 0

    var compilerMessages: List<CompilerMessage> = emptyList()
        set(value) {
            checkIsEventDispatchThread()
            field = value
            if (compilerMessages.isEmpty()) {
                errorListUI.setListData(emptyArray())
                messageUI.text = ""
            } else {
                val titles = compilerMessages.map { it.name + prettyRegion(it.messageWithRegion.region) + "    " + it.messageWithRegion.title }
                errorListUI.setListData(titles.toTypedArray())
                messageUI.text = compilerMessages[0].messageWithRegion.message
                indexCompilerMessages = 0
            }
        }

    private fun prettyRegion(region: Region): String {
        return " @ line ${region.start.line} column ${region.start.column}"
    }

    init {
        setToolbar(createToolbar())

        val splitPane = JBSplitter()
        splitPane.firstComponent = JBScrollPane(errorListUI)
        splitPane.secondComponent = messageUI
        setContent(splitPane)

        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(messages: List<CompilerMessage>) {
                    ApplicationManager.getApplication().invokeLater {
                        compilerMessages = messages
                        contentManager.getContent(0)?.displayName = "${compilerMessages.size} errors"
                        errorListUI.selectedIndex = 0
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
                    var filePath = compilerMessages[indexCompilerMessages].path
                    if (!filePath.startsWith("/")) filePath = project.basePath + "/" + filePath
                    val file = LocalFileSystem.getInstance().findFileByPath(Paths.get(filePath)) // TODO differently ?
                    val start = compilerMessages[indexCompilerMessages].messageWithRegion.region.start
                    OpenFileDescriptor(project, file!!, start.line - 1, start.column - 1)
                } else {
                    null
                }
            }
            else ->
                super.getData(dataId)
        }
    }
}
