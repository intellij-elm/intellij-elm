package org.elm.ide.inspections.import

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmUnresolvedReferenceInspection
import org.intellij.lang.annotations.Language

class AddQualifierFixTest : ElmInspectionsTestBase(ElmUnresolvedReferenceInspection()) {
    fun `test value`() = check(
            """
--@ main.elm
import Foo
main = bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            """
import Foo
main = Foo.bar
""")

    fun `test type`() = check(
            """
--@ main.elm
import Foo
bar : Bar{-caret-} -> ()
bar = ()
--@ Foo.elm
module Foo exposing (Bar)
type Bar = Bar
""",
            """
import Foo
bar : Foo.Bar -> ()
bar = ()
""")

    fun `test constructor`() = check(
            """
--@ main.elm
import Foo
main = BarVariant{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = BarVariant
""",
            """
import Foo
main = Foo.BarVariant
""")

    fun `test pattern`() = check(
            """
--@ main.elm
import Foo
main : Foo.Bar -> ()
main b =
  case b of
      BarVariant{-caret-} -> ()
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = BarVariant
""",
            """
import Foo
main : Foo.Bar -> ()
main b =
  case b of
      Foo.BarVariant{-caret-} -> ()
""")

    fun `test qualified value`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = Foo.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    fun `test qualified type`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
f : Foo.Bar{-caret-} -> ()
f bar = ()
--@ Foo.elm
module Foo exposing (Bar)
type Bar = BarVariant
""")


    fun `test qualified constructor`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = Foo.BarVariant{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = BarVariant
""")

    fun `test value with import alias`() = check(
            """
--@ main.elm
import Foo as F
main = bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            """
import Foo as F
main = F.bar
""")


    fun `test multiple qualifier candidates`() = checkFixByTextWithMultipleChoice(
            """
--@ main.elm
import Foo
import Bar
import Baz
main = quux{-caret-}
--@ Foo.elm
module Foo exposing (quux)
quux = ()
--@ Bar.elm
module Bar exposing (quux)
quux = ()
--@ Baz.elm
module Baz exposing (somethingElse)
somethingElse = ()
""",
            listOf("Foo.", "Bar."),
            "Bar.",
            """
import Foo
import Bar
import Baz
main = Bar.quux
""")

    fun `test multiple qualifier candidates with aliases`() = checkFixByTextWithMultipleChoice(
            """
--@ main.elm
import Foo
import Bar as B
main = quux{-caret-}
--@ Foo.elm
module Foo exposing (quux)
quux = ()
--@ Bar.elm
module Bar exposing (quux)
quux = ()
""",
            listOf("Foo.", "B."),
            "B.",
            """
import Foo
import Bar as B
main = B.quux
""")

    fun `test binary infix operator`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = 2 <error>**{-caret-}</error> 3
--@ Foo.elm
module Foo exposing ((**))
infix right 5 (**) = power
power a b = List.product (List.repeat b a)
""")

    fun `test unavailable when value not exposed`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = <error>bar{-caret-}</error>
--@ Foo.elm
module Foo exposing (quux)
bar = 42
quux = 0
""")

    fun `test unavailable when qualified ref alias is not possible`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = <error>Foo.Bogus.bar{-caret-}</error>
--@ Foo.elm
module Foo exposing (bar)
bar = 0
""")

    fun `test unavailable on type annotation when local function hides external name`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
bar{-caret-} : ()
bar = ()
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")

    fun `test binary infix operator containing dot is never qualified`() = checkFixIsUnavailableByFileTree("Qualify name",
            """
--@ main.elm
import Foo
main = 2 <error>|.{-caret-}</error> 3
--@ Foo.elm
module Foo exposing (..)
infix right 5 (|.) = power
power a b = List.product (List.repeat b a)
""")


    private fun check(@Language("Elm") before: String, @Language("Elm") after: String) {
        val testProject = fileTreeFromText(before).createAndOpenFileWithCaretMarker()
        enableInspection()
        applyQuickFix("Qualify name")

        // adding a qualifier must be done using stubs only
        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })

        myFixture.checkResult(replaceCaretMarker(after).trim())
    }

    private fun checkFixByTextWithMultipleChoice(
            @Language("Elm") before: String,
            expectedElements: List<String>,
            choice: String,
            @Language("Elm") after: String
    ) {
        var chooseItemWasCalled = false

        withMockQualifierPickerUI(object : QualifierPickerUI {
            override fun choose(qualifiers: List<String>, callback: (String) -> Unit) {
                chooseItemWasCalled = true
                assertEquals(qualifiers, expectedElements)
                assertContainsElements(qualifiers, choice)
                callback(choice)
            }
        }) { check(before, after) }

        check(chooseItemWasCalled) { "`chooseItem` was not called" }
    }
}
