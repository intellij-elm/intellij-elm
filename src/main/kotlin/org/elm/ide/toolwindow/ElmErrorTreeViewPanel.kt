package org.elm.ide.toolwindow

import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.event.TreeSelectionListener

class ElmErrorTreeViewPanel(project: Project?, helpId: String?, createExitAction: Boolean, createToolbar: Boolean) : NewErrorTreeViewPanel(project, helpId, createExitAction, createToolbar) {

    val messages = mutableListOf<String>()

    override fun addMessage(type: Int, text: Array<out String>, file: VirtualFile?, line: Int, column: Int, data: Any?) {
        super.addMessage(type, text, file, line, column, data)
        messages.add(data as String)
    }

    fun addSelectionListener(tsl: TreeSelectionListener) {
        myTree.addTreeSelectionListener(tsl)
    }
}

fun connectFriendlyMessages(project: Project, errorTreeViewPanel: ElmErrorTreeViewPanel) {
    ToolWindowManager.getInstance(project).getToolWindow("Friendly Messages")?.let {
        val reportUI = (it.contentManager.contents[0].component as ReportPanel).reportUI
        reportUI.background = errorTreeViewPanel.background
        errorTreeViewPanel.addSelectionListener(ErrorTreeSelectionListener(errorTreeViewPanel, reportUI, it))
        reportUI.text = ""
    }
}
