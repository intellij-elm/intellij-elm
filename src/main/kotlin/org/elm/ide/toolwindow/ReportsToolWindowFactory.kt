package org.elm.ide.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ReportsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(toolWindow.contentManager) {
            removeAllContents(true)
            addContent(factory.createContent(ReportPanel(), "Report", true))
        }
    }
}