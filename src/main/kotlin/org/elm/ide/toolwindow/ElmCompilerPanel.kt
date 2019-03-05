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
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import org.elm.openapiext.checkIsEventDispatchThread
import org.elm.openapiext.toPsiFile
import org.elm.workspace.compiler.CompilerMessage
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.Region
import org.elm.workspace.compiler.Start
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextPane
import javax.swing.ListSelectionModel


class ElmCompilerPanel(private val project: Project, private val contentManager: ContentManager) : SimpleToolWindowPanel(true, false), Disposable, OccurenceNavigator {

    private fun occurenceInfo(): OccurenceNavigator.OccurenceInfo {
        val (virtualFile, document, start) = startFromErrorMessage(project.guessProjectDir())
        document?.let {
            val offset = it.getLineStartOffset(start.line) + start.column - 1
            virtualFile?.let {
                return OccurenceNavigator.OccurenceInfo(PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, offset), -1, -1)
            }
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
        font = JBUI.Fonts.create("Droid Sans Mono", 12)
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
                    val (virtualFile, _, start) = startFromErrorMessage(project.guessProjectDir())
                    virtualFile?.let {
                        return OpenFileDescriptor(project, virtualFile, start.line - 1, start.column - 1)
                    }
                    throw RuntimeException("The impossible happened...")
                } else {
                    null
                }
            }
            else ->
                super.getData(dataId)
        }
    }

    private fun startFromErrorMessage(baseDir: VirtualFile?): Triple<VirtualFile?, Document?, Start> {
        val path = compilerMessages[indexCompilerMessages].path
        val virtualFile: VirtualFile?
        if (FileUtil.isAbsolute(path))
            virtualFile = LocalFileSystem.getInstance().findFileByPath(path)
        else {
            virtualFile = VfsUtil.findRelativeFile(path, baseDir)
        }
        val psiFile = virtualFile?.toPsiFile(project)
        psiFile?.let {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            val start = compilerMessages[indexCompilerMessages].messageWithRegion.region.start
            return Triple(virtualFile, document, start)
        }
        throw RuntimeException("The impossible happened...")
    }

}
