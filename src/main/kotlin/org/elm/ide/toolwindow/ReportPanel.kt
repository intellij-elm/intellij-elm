package org.elm.ide.toolwindow

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ScrollPaneFactory
import javax.swing.JTextPane


class ReportPanel : SimpleToolWindowPanel(true, false) {

    val reportUI = JTextPane().apply {
        contentType = "text/html"
        isEditable = false
        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    init {
        setContent(ScrollPaneFactory.createScrollPane(reportUI, 0))
    }

}
