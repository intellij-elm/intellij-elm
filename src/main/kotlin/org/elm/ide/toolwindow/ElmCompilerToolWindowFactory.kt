package org.elm.ide.toolwindow

import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.containers.toArray
import com.intellij.util.ui.MessageCategory
import org.elm.openapiext.findFileByPath
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.ElmError
import java.nio.file.Path


/**
 * Boilerplate to connect tool window content to IntelliJ.
 */
class ElmCompilerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String, offset: Int) {
                    val errorTreeViewPanel = NewErrorTreeViewPanel(project, "Elm Compiler", false, true)
                    for (compilerMessage in messages) {
                        val sourceLocation = compilerMessage.location!!
                        val virtualFile = baseDirPath.resolve(sourceLocation.path).let {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        errorTreeViewPanel.addMessage(MessageCategory.ERROR, compilerMessage.messages.toArray(emptyArray()), virtualFile,
                            sourceLocation.region?.start?.let { it.line - 1 } ?: 0,
                            sourceLocation.region?.start?.let { it.column - 1 } ?: 0,
                            null)
                    }

                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(ContentImpl(errorTreeViewPanel, "Compilation result", false))

                    ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler")?.activate(null);
                }
            })
        }
    }
}
