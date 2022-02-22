package org.elm.ide.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.ui.MessageCategory
import org.elm.ide.actions.ElmExternalReviewAction
import org.elm.openapiext.findFileByPath
import org.elm.workspace.elmreview.ElmReviewError
import java.nio.file.Path

class ElmReviewToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(project.messageBus.connect()) {
            subscribe(ElmExternalReviewAction.ERRORS_TOPIC, object : ElmExternalReviewAction.ElmReviewErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmReviewError>, targetPath: String?, offset: Int) {

                    val errorTreeViewPanel = ElmErrorTreeViewPanel(project, "elm-review", createExitAction = false, createToolbar = true)

                    messages.forEachIndexed { index, elmReviewError ->
                        val sourceLocation = elmReviewError.path!!
                        val virtualFile = baseDirPath.resolve(sourceLocation).let {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        val encodedIndex = "\u200B".repeat(index)
                        errorTreeViewPanel.addMessage(
                            MessageCategory.SIMPLE, arrayOf("$encodedIndex${elmReviewError.rule}:", "${elmReviewError.message}"),
                            virtualFile,
                            elmReviewError.region.start.let { it.line - 1 },
                            elmReviewError.region.start.let { it.column - 1 },
                            elmReviewError.html
                        )
                    }

                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(ContentImpl(errorTreeViewPanel, "Elm-Review Result", true))
                    toolWindow.show(null)
                    errorTreeViewPanel.expandAll()
                    errorTreeViewPanel.requestFocus()
                    focusEditor(project)
                }
            })
        }
    }
}
