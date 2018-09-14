package org.elm.workspace

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.elm.openapiext.saveAllDocuments

class ElmBuildAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
                ?: return
        saveAllDocuments()

        val compilerPath = project.elmToolchain?.elmCompilerPath
        if (compilerPath == null) {
            Messages.showErrorDialog("No path to the Elm compiler", "Build Error")
            return
        }

        val toolWindowId = "org.elm.build"
        val existingToolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)
        val toolWindow = if (existingToolWindow == null) {
            ToolWindowManager.getInstance(project).registerToolWindow(toolWindowId, true, ToolWindowAnchor.BOTTOM)
        } else {
            existingToolWindow.contentManager.removeAllContents(true)
            existingToolWindow
        }

        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        val content = ContentFactory.SERVICE.getInstance().createContent(consoleView.component, "Elm Build", false)
        toolWindow.contentManager.addContent(content)

        val elmProject = project.elmWorkspace.allProjects.first()
        val processOutput = ElmCLI(compilerPath).make(project, elmProject)

        consoleView.clear()
        consoleView.print(processOutput.stdout, ConsoleViewContentType.NORMAL_OUTPUT)
        consoleView.print(processOutput.stderr, ConsoleViewContentType.ERROR_OUTPUT)
    }
}
