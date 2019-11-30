/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.utils

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.vcs.changes.issueLinks.LinkMouseListenerBase
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.util.regex.Pattern
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/*
 * IntelliJ has a new UI DSL (http://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html)
 * but it has been evolving a lot and there are API incompatibilities between 2019.2
 * and previous releases. So I've decided to use this simple layout util from intellij-rust
 * until we can fully adopt 2019.2 as our minimum IDE version. At which time, this file
 * can and should be deleted.
 */

const val HGAP = 30
const val VERTICAL_OFFSET = 2
const val HORIZONTAL_OFFSET = 5


fun layout(block: ElmLayoutUIBuilder.() -> Unit): JPanel {
    val panel = JPanel(BorderLayout())
    val innerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    panel.add(innerPanel, BorderLayout.NORTH)
    val builder = ElmLayoutUIBuilderImpl(innerPanel).apply(block)
    UIUtil.mergeComponentsWithAnchor(builder.labeledComponents)
    return panel
}


interface ElmLayoutUIBuilder {
    fun block(text: String, block: ElmLayoutUIBuilder.() -> Unit)
    fun row(text: String = "", component: JComponent, toolTip: String = "")
    fun noteRow(text: String)
}


private class ElmLayoutUIBuilderImpl(
        val panel: JPanel,
        val labeledComponents: MutableList<LabeledComponent<*>> = mutableListOf()
) : ElmLayoutUIBuilder {

    override fun block(text: String, block: ElmLayoutUIBuilder.() -> Unit) {
        val blockPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = IdeBorderFactory.createTitledBorder(text, false)
        }
        ElmLayoutUIBuilderImpl(blockPanel, labeledComponents).apply(block)
        panel.add(blockPanel)
    }

    override fun row(text: String, component: JComponent, toolTip: String) {
        val labeledComponent = LabeledComponent.create(component, text, BorderLayout.WEST).apply {
            (layout as? BorderLayout)?.hgap = HGAP
            border = JBUI.Borders.empty(VERTICAL_OFFSET, HORIZONTAL_OFFSET)
            toolTipText = toolTip.trimIndent()
        }
        labeledComponents += labeledComponent
        panel.add(labeledComponent)
    }

    private val HREF_PATTERN =
            Pattern.compile("<a(?:\\s+href\\s*=\\s*[\"']([^\"']*)[\"'])?\\s*>([^<]*)</a>")

    private val LINK_TEXT_ATTRIBUTES: SimpleTextAttributes
        get() = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkColor())


    override fun noteRow(text: String) {
        val noteComponent = SimpleColoredComponent()
        val matcher = HREF_PATTERN.matcher(text)
        if (!matcher.find()) {
            noteComponent.append(text)
            panel.add(noteComponent)
            return
        }

        var prev = 0
        do {
            if (matcher.start() != prev) {
                noteComponent.append(text.substring(prev, matcher.start()))
            }

            val linkUrl = matcher.group(1)
            noteComponent.append(
                    matcher.group(2),
                    LINK_TEXT_ATTRIBUTES,
                    SimpleColoredComponent.BrowserLauncherTag(linkUrl)
            )
            prev = matcher.end()
        } while (matcher.find())

        LinkMouseListenerBase.installSingleTagOn(noteComponent)

        if (prev < text.length) {
            noteComponent.append(text.substring(prev))
        }

        panel.add(noteComponent)
    }
}
