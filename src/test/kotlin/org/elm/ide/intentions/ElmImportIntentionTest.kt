package org.elm.ide.intentions

import org.elm.fileTreeFromText
import org.intellij.lang.annotations.Language

class ElmImportIntentionTest: ElmIntentionTestBase(ElmImportIntentionAction()) {


    fun `test un-qualified value`() = check(
"""
--@ main.elm
main = bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
"""
import Foo exposing (bar)
main = bar
""")


    fun `test qualified value`() = check(
"""
--@ main.elm
main = Foo.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
"""
import Foo
main = Foo.bar
""")


    fun `test verify unavailable when value not exposed`() = verifyUnavailable(
"""
--@ main.elm
main = bar{-caret-}
--@ Foo.elm
module Foo exposing ()
bar = 42
""")

    protected fun check(@Language("Elm") before: String, @Language("Elm") after: String) {
        fileTreeFromText(before).createAndOpenFileWithCaretMarker()
        myFixture.launchAction(intention)
        myFixture.checkResult(replaceCaretMarker(after).trim())
    }

    protected fun verifyUnavailable(@Language("Elm") before: String) {
        fileTreeFromText(before).createAndOpenFileWithCaretMarker()
        check(intention.familyName !in myFixture.availableIntentions.mapNotNull { it.familyName }) {
            "\"$intention\" intention should not be available"
        }
    }
}