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

    fun `test no hints for destructured record arguments`() = checkByText("""module Foo exposing (..)

example =
    nameToString
        { first = "Jane"
        , last = "Doe"
        }


nameToString { first, last } =
    first ++ " " ++ last
""")

    fun `test no hints for discarded arguments`() = checkByText("""module Foo exposing (..)

example =
    getUserId token <hint text="retry:"/>True

getUserId : Token -> Bool -> Cmd msg
getUserId _ retry =
    getUserIdHttpRequest retry
""")

    fun `test single constructor argument destructure`() = checkByText("""module Foo exposing (..)

example user =
    tokenHeader <hint text="token:"/>user.credentials

type Token = Token String

tokenHeader : Token -> Cmd msg
tokenHeader (Token token) =
    header "token" token
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