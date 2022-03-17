package org.elm.ide.toolwindow

import com.intellij.ide.errorTreeView.ErrorTreeNodeDescriptor
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JTextPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class ErrorTreeSelectionListener(private val messages: List<String>, private val reportUI: JTextPane, val toolWindow: ToolWindow) : TreeSelectionListener {

    override fun valueChanged(e: TreeSelectionEvent) {
        val collectSelectedUserObject = TreeUtil.collectSelectedUserObjects(e.source as Tree)[0] as ErrorTreeNodeDescriptor
        val prefixedErrorMessage = collectSelectedUserObject.element.text[0]
        val index = prefixedErrorMessage.count { it == '​' }
        if (index >= 0 && index < messages.size) {
            reportUI.text = messages[index]
            reportUI.caretPosition = 0
            toolWindow.show(null)
        }
    }
}