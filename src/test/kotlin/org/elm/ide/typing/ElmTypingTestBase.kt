/*
* Use of this source code is governed by the MIT license that can be
* found in the LICENSE file.
*
* Originally from intellij-rust
*/

package org.elm.ide.typing

import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

abstract class ElmTypingTestBase : ElmTestBase() {
    protected fun doTest(c: Char = '\n') = checkByFile {
        myFixture.type(c)
    }

    protected fun doTestByText(@Language("Elm") before: String,
                               @Language("Elm") after: String,
                               c: Char = '\n') =
            checkByText(before.trimIndent(), after.trimIndent()) {
                myFixture.type(c)
            }
}
