package org.elm.ide.toolwindow

import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.ui.MessageCategory
import org.elm.ide.actions.ElmExternalReviewAction
import org.elm.openapiext.findFileByPath
import org.elm.workspace.elmreview.ElmReviewError
import java.nio.file.Path

/**
 * Boilerplate to connect tool window content to IntelliJ.
 */
class ElmReviewToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(project.messageBus.connect()) {
            subscribe(ElmExternalReviewAction.ERRORS_TOPIC, object : ElmExternalReviewAction.ElmReviewErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmReviewError>, targetPath: String?, offset: Int) {
                    val errorTreeViewPanel = NewErrorTreeViewPanel(project, "elm-review", false, true)

                    for (reviewMessage in messages) {
                        val sourceLocation = reviewMessage.path
                        val virtualFile = baseDirPath.resolve(sourceLocation).let {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        errorTreeViewPanel.addMessage(MessageCategory.WARNING, arrayOf(reviewMessage.message), virtualFile,
                            reviewMessage.region.start.let { it.line - 1 },
                            reviewMessage.region.start.let { it.column - 1 },
                            null)

                    }
                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(ContentImpl(errorTreeViewPanel, "elm-review result", false))

                    ToolWindowManager.getInstance(project).getToolWindow("elm-review")?.activate(null);
                }
            })
        }
    }
}