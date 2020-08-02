/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.elm.ide.hints.parameter

import com.intellij.codeInsight.daemon.impl.HintRenderer
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

class ElmInlayParameterHintsProviderTest : ElmTestBase() {
    fun `test fn args`() = checkByText("""module Foo exposing (..)

greet first last = "Hello " ++ first ++ " " ++ last

greeting =
    greet {-hint text="first:"-}"Jane" {-hint text="last:"-}"Doe"

""")

    @Suppress("UnstableApiUsage")
    private fun checkByText(@Language("Elm") code: String) {
        InlineFile(code.replace(HINT_COMMENT_PATTERN, "<$1/>"))

        ElmInlayParameterHints.enabledOption.set(true)
        ElmInlayParameterHints.smartOption.set(true)

        myFixture.testInlays({ (it.renderer as HintRenderer).text }) { it.renderer is HintRenderer }
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""\{-(hint.*?)-\}""")
    }
}