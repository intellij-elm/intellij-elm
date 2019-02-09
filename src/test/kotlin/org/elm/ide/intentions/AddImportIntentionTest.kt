package org.elm.ide.intentions

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.intellij.lang.annotations.Language

class AddImportIntentionTest : ElmIntentionTestBase(AddImportIntention()) {


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


    fun `test annotation value`() = check(
            """
--@ main.elm
bar : Bar{-caret-} -> Bar
bar = ()
--@ Foo.elm
module Foo exposing (Bar)
type Bar = Bar
""",
            """
import Foo exposing (Bar)
bar : Bar -> Bar
bar = ()
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


    fun `test qualified type`() = check(
            """
--@ main.elm
f : Foo.Bar{-caret-} -> ()
f bar = ()
--@ Foo.elm
module Foo exposing (Bar)
type Bar = BarVariant
""",
            """
import Foo
f : Foo.Bar -> ()
f bar = ()
""")


    fun `test qualified union constructor`() = check(
            """
--@ main.elm
main = Foo.BarVariant{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = BarVariant
""",
            """
import Foo
main = Foo.BarVariant
""")


    // see https://github.com/klazuka/intellij-elm/issues/77
    fun `test importing a union variant constructor exposes all variants`() = check(
            """
--@ main.elm
main = BarVariant{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = BarVariant ()
""",
            """
import Foo exposing (Bar(..))
main = BarVariant
""")


    fun `test qualified value using an import alias`() = check(
            """
--@ main.elm
main = Foo.bar{-caret-}
--@ FooTooLongToType.elm
module FooTooLongToType exposing (bar)
bar = 42
""",
            """
import FooTooLongToType as Foo
main = Foo.bar
""")


    fun `test binary infix operator`() = check(
            """
--@ main.elm
main = 2 **{-caret-} 3
--@ Foo.elm
module Foo exposing ((**))
infix right 5 (**) = power
power a b = List.product (List.repeat b a)
""",
            """
import Foo exposing ((**))
main = 2 ** 3
""")


    fun `test inserts import between module decl and value-decl`() = check(
            """
--@ main.elm
module Main exposing (..)
main = Foo.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            """
module Main exposing (..)
import Foo
main = Foo.bar{-caret-}
""")


    fun `test inserts import between module decl and doc-comment`() = check(
            """
--@ main.elm
module Main exposing (..)
{-| this is a doc comment. it must be above imports -}
main = Foo.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            """
module Main exposing (..)
{-| this is a doc comment. it must be above imports -}
import Foo
main = Foo.bar{-caret-}
""")


    fun `test expose a value with existing import`() = check(
            """
--@ main.elm
import Foo
main = bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            """
import Foo exposing (bar)
main = bar
""")


    fun `test expose an infix operator with existing import`() = check(
            """
--@ main.elm
import Foo
main = 2 **{-caret-} 3
--@ Foo.elm
module Foo exposing ((**))
infix right 5 (**) = power
power a b = List.product (List.repeat b a)
""",
            """
import Foo exposing ((**))
main = 2 ** 3
""")


    fun `test merge with existing exposed values`() = check(
            """
--@ main.elm
import Foo exposing (quux)
main = quux + bar{-caret-}
--@ Foo.elm
module Foo exposing (bar, quux)
bar = 42
quux = 99
""",
            """
import Foo exposing (bar, quux)
main = quux + bar
""")


    fun `test merge with existing exposed union type`() = check(
            """
--@ main.elm
import App exposing (Page)
main = Settings{-caret-}
--@ App.elm
module App exposing (Page(..))
type Page = Home | Settings
""",
            """
import App exposing (Page(..))
main = Settings
""")


    fun `test inserts import after existing import`() = check(
            """
--@ main.elm
import Foo exposing (bar)
main = bar + quux{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
--@ Quux.elm
module Quux exposing (quux)
quux = 99
""",
            """
import Foo exposing (bar)
import Quux exposing (quux)
main = bar + quux
""")


    fun `test verify unavailable when value not exposed`() = verifyUnavailable(
            """
--@ main.elm
main = bar{-caret-}
--@ Foo.elm
module Foo exposing (quux)
bar = 42
quux = 0
""")

    fun `test verify unavailable when value not exposed (qualified ref)`() = verifyUnavailable(
            """
--@ main.elm
main = Foo.bar{-caret-}
--@ Foo.elm
module Foo exposing (quux)
bar = 42
quux = 0
""")

    fun `test verify unavailable when qualified ref alias is not possible`() = verifyUnavailable(
            """
--@ main.elm
main = Foo.Bogus.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 0
""")

    fun `test verify unavailable on type annotation when local function hides external name`() = verifyUnavailable(
            """
--@ main.elm
bar{-caret-} : Int -> Int
bar = ()
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")

    fun `test binary infix operator containing dot is never qualified`() = check(
            """
--@ main.elm
main = 2 |.{-caret-} 3
--@ Foo.elm
module Foo exposing (..)
infix right 5 (|.) = power
power a b = List.product (List.repeat b a)
""",
            """
import Foo exposing ((|.))
main = 2 |. 3
""")


    protected fun check(@Language("Elm") before: String, @Language("Elm") after: String) {
        val testProject = fileTreeFromText(before).createAndOpenFileWithCaretMarker()

        // auto-adding an import must be done using stubs only
        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })

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
