package org.elm.ide.inspections.import

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmUnresolvedReferenceInspection
import org.elm.lang.core.imports.ImportAdder.Import
import org.intellij.lang.annotations.Language

class AddImportFixTest : ElmInspectionsTestBase(ElmUnresolvedReferenceInspection()) {

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

    fun `test importing a union variant constructor with same name as union in value expression`() = check(
            """
--@ main.elm
main = Bar{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = Bar
""",
            """
import Foo exposing (Bar(..))
main = Bar
""")

    fun `test importing a union with same name as constructor in type expression`() = check(
            """
--@ main.elm
main : Bar{-caret-}
--@ Foo.elm
module Foo exposing (Bar(..))
type Bar = Bar
""",
            """
import Foo exposing (Bar)
main : Bar
""")

    fun `test importing a type on the RHS of a type alias declaration`() = check(
            """
--@ main.elm
type alias Foo = Bar{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Bar = ()
""",
            """
import Foo exposing (Bar)
type alias Foo = Bar
""")

    fun `test importing a type on the RHS of a union type declaration`() = check(
            """
--@ main.elm
type Foo = Foo Bar{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Bar = ()
""",
            """
import Foo exposing (Bar)
type Foo = Foo Bar
""")

    fun `test multiple import candidates`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
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
            listOf("Bar", "Foo"),
            "Foo",
            """
import Foo exposing (quux)
main = quux
""")

    fun `test multiple import candidates sort order, exact match`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
main = Two.foo{-caret-}
--@ Utils/One.elm
module Utils.One exposing (foo)
foo = ()
--@ Two.elm
module Two exposing (foo)
foo = ()
--@ Three.elm
module Three exposing (foo)
foo = ()
""",
            listOf("Two", "Three", "Utils.One"),
            "Two",
            """
import Two
main = Two.foo
""")

    fun `test multiple import candidates sort order, substring match`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
main = One.foo{-caret-}
--@ Utils/One.elm
module Utils.One exposing (foo)
foo = ()
--@ Utils/Two.elm
module Utils.Two exposing (foo)
foo = ()
--@ Three.elm
module Three exposing (foo)
foo = ()
""",
            listOf("Utils.One", "Three", "Utils.Two"),
            "Utils.One",
            """
import Utils.One as One
main = One.foo
""")

    fun `test multiple import candidates sort order, subsequence match 1`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
main : JD.Value{-caret-}
--@ Json/Encode.elm
module Json.Encode exposing (Value)
type Value = V
--@ Json/Decode.elm
module Json.Decode exposing (Value)
type Value = V
""",
            listOf("Json.Decode", "Json.Encode"),
            "Json.Decode",
            """
import Json.Decode as JD
main : JD.Value
""")

    fun `test multiple import candidates sort order, subsequence match 2`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
main : JE.Value{-caret-}
--@ Json/Encode.elm
module Json.Encode exposing (Value)
type Value = V
--@ Json/Decode.elm
module Json.Decode exposing (Value)
type Value = V
""",
            listOf("Json.Encode", "Json.Decode"),
            "Json.Encode",
            """
import Json.Encode as JE
main : JE.Value
""")


    // NOTE: must use the "multiple choice" test because in order to give the user more visibility
    // about what's going on, I've decided to always show the picker when importing using an alias.
    fun `test qualified value using an import alias`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
main = Foo.bar{-caret-}
--@ FooTooLongToType.elm
module FooTooLongToType exposing (bar)
bar = 42
""",
            listOf("FooTooLongToType"),
            "FooTooLongToType",
            """
import FooTooLongToType as Foo
main = Foo.bar
""")

    fun `test adding alias to existing import`() = checkAutoImportFixByTextWithMultipleChoice(
            """
--@ main.elm
import Foo
main = Fob.bar{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""",
            listOf("Foo"),
            "Foo",
            """
import Foo as Fob
main = Fob.bar
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

    fun `test verify unavailable when value not exposed`() = checkFixIsUnavailableByFileTree("Import",
            """
--@ main.elm
main = <error>bar</error>{-caret-}
--@ Foo.elm
module Foo exposing (quux)
bar = 42
quux = 0
""")

    fun `test verify unavailable when value not exposed (qualified ref)`() = checkFixIsUnavailableByFileTree("Import",
            """
--@ main.elm
main = <error>Foo.bar</error>{-caret-}
--@ Foo.elm
module Foo exposing (quux)
bar = 42
quux = 0
""")

    fun `test verify unavailable when qualified ref alias is not possible`() = checkFixIsUnavailableByFileTree("Import",
            """
--@ main.elm
main = <error>Foo.Bogus.bar</error>{-caret-}
--@ Foo.elm
module Foo exposing (bar)
bar = 0
""")

    fun `test verify unavailable on type annotation when local function hides external name`() = checkFixIsUnavailableByFileTree("Import",
            """
--@ main.elm
bar{-caret-} : ()
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

    private fun check(@Language("Elm") before: String, @Language("Elm") after: String) {
        val testProject = fileTreeFromText(before).createAndOpenFileWithCaretMarker()
        enableInspection()
        applyQuickFix("Import")
        // auto-adding an import must be done using stubs only
        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })

        myFixture.checkResult(replaceCaretMarker(after).trim())
    }

    private fun checkAutoImportFixByTextWithMultipleChoice(
            @Language("Elm") before: String,
            expectedElements: List<String>,
            choice: String,
            @Language("Elm") after: String
    ) {
        var chooseItemWasCalled = false

        withMockImportPickerUI(object : ImportPickerUI {
            override fun choose(candidates: List<Import>, callback: (Import) -> Unit) {
                chooseItemWasCalled = true
                val actualItems = candidates.map { it.moduleName }
                assertEquals(expectedElements, actualItems)
                val selectedValue = candidates.find { it.moduleName == choice }
                        ?: error("Can't find `$choice` in `$actualItems`")
                callback(selectedValue)
            }
        }) { check(before, after) }

        check(chooseItemWasCalled) { "`chooseItem` was not called" }
    }
}
