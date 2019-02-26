package org.elm.ide.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Boilerplate to connect tool window content to IntelliJ.
 */
class ElmCompilerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(toolWindow.contentManager) {
            addContent(factory.createContent(ElmCompilerPanel(project, toolWindow.contentManager), "", true))
        }
    }
}