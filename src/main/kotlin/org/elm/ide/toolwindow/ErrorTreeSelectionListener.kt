package org.elm.ide.toolwindow

import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.treeStructure.Tree
import javax.swing.JTextPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class ErrorTreeSelectionListener(private val errorTreeViewPanel: ElmErrorTreeViewPanel, private val reportUI: JTextPane, val toolWindow: ToolWindow) : TreeSelectionListener {

    override fun valueChanged(e: TreeSelectionEvent) {
        val index = (e.source as Tree).selectionModel.leadSelectionRow - 1
        reportUI.text = errorTreeViewPanel.messages[index]
        reportUI.caretPosition = 0
        toolWindow.activate(null)
    }
}