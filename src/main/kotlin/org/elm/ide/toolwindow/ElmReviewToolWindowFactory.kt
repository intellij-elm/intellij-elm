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
                    connectFriendlyMessages(project, errorTreeViewPanel)

                    for (message in messages) {
                        val sourceLocation = message.path!!
                        val virtualFile = baseDirPath.resolve(sourceLocation).let {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        errorTreeViewPanel.addMessage(
                            MessageCategory.WARNING, arrayOf(message.message),
                            virtualFile,
                            message.region.start.let { it.line - 1 },
                            message.region.start.let { it.column - 1 },
                            message.html
                        )
                    }
                    errorTreeViewPanel.expandAll()
                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(ContentImpl(errorTreeViewPanel, "Elm-Review Result", true))
                    toolWindow.activate(null)
                }
            })
        }
    }
}
