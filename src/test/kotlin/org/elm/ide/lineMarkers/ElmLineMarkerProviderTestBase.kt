/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.ide.lineMarkers

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language


abstract class ElmLineMarkerProviderTestBase : ElmTestBase() {

    protected fun doTestByText(@Language("Elm") source: String) {
        myFixture.configureByText("Main.elm", source)
        myFixture.doHighlighting()
        val expected = markersFrom(source)
        val actual = markersFrom(myFixture.editor, myFixture.project)
        assertEquals(expected.joinToString(COMPARE_SEPARATOR), actual.joinToString(COMPARE_SEPARATOR))
    }

    private fun markersFrom(text: String) =
            text.split('\n')
                    .withIndex()
                    .filter { it.value.contains(MARKER) }
                    .map { Pair(it.index, it.value.substring(it.value.indexOf(MARKER) + MARKER.length).trim()) }

    private fun markersFrom(editor: Editor, project: Project) =
            DaemonCodeAnalyzerImpl.getLineMarkers(editor.document, project)
                    .map {
                        Pair(editor.document.getLineNumber(it.element?.textRange?.startOffset as Int),
                                it.lineMarkerTooltip)
                    }
                    .sortedBy { it.first }

    private companion object {
        const val MARKER = "--> "
        const val COMPARE_SEPARATOR = " | "
    }
}
