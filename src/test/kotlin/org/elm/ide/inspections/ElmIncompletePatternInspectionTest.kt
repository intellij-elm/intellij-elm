package org.elm.ide.inspections

import org.intellij.lang.annotations.Language

class ElmIncompletePatternInspectionTest : ElmInspectionsTestBase(ElmIncompletePatternInspection()) {

    fun `test no existing branch`() = checkFixByText("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar ->
            --EOL

        Baz ->
            --EOL

        Qux ->
            --EOL
""")

    fun `test one existing branch`() = checkFixByText("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
        Baz ->
            ()
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Baz ->
            ()

        Bar ->
            --EOL

        Qux ->
            --EOL
""")

    fun `test two existing branches`() = checkFixByText("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
        Baz ->
            ()

        Qux ->
            ()
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Baz ->
            ()

        Qux ->
            ()

        Bar ->
            --EOL
""")

    fun `test params`() = checkFixByText("""
type Foo = Foo
type alias BarBaz = Foo
type Maybe a = Just a | Nothing
type Msg a b
    = MsgOne Foo BarBaz
    | MsgTwo (Maybe (Maybe Foo))
    | MsgThree b a
    | MsgFour {x: ()}
    | MsgFive (x, y)
    | MsgSix (Msg () ())

foo : Msg a b -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
type Foo = Foo
type alias BarBaz = Foo
type Maybe a = Just a | Nothing
type Msg a b
    = MsgOne Foo BarBaz
    | MsgTwo (Maybe (Maybe Foo))
    | MsgThree b a
    | MsgFour {x: ()}
    | MsgFive (x, y)
    | MsgSix (Msg () ())

foo : Msg a b -> ()
foo it =
    case it of
        MsgOne foo barBaz ->
            --EOL

        MsgTwo maybe ->
            --EOL

        MsgThree b a ->
            --EOL

        MsgFour record ->
            --EOL

        MsgFive (x, y) ->
            --EOL

        MsgSix msg ->
            --EOL
""")

    fun `test one existing branch, wildcard pattern`() = checkFixByText("Add '_' branch", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
        Baz ->
            ()
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Baz ->
            ()

        _ ->
            --EOL
""".replace("--EOL", ""))

    fun `test no leading whitespace`() = checkFixByText("""
type Foo = Bar

foo : Foo -> ()
foo it =<error>case{-caret-}</error> it of
""", """
type Foo = Bar

foo : Foo -> ()
foo it =case it of
        Bar ->
            --EOL
""")

    fun `test nesting in let`() = checkFixByText("""
type Foo = Bar

foo : Foo -> ()
foo it =
    let
        bar =
            <error>case{-caret-}</error> it of
    in
        ()
""", """
type Foo = Bar

foo : Foo -> ()
foo it =
    let
        bar =
            case it of
                Bar ->
                    --EOL
    in
        ()
""")

    fun `test nesting in case`() = checkFixByText("""
type Foo = Bar

foo : Foo -> ()
foo it =
    case () of
        () ->
            <error>case{-caret-}</error> it of
""", """
type Foo = Bar

foo : Foo -> ()
foo it =
    case () of
        () ->
            case it of
                Bar ->
                    --EOL
""")

    fun `test qualified import`() = checkFixByFileTree("""
--@ Data/User.elm
module Data.User exposing (..)
type Foo = Bar | Baz | Qux

--@ main.elm
import Data.User
foo : Data.User.Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
import Data.User
foo : Data.User.Foo -> ()
foo it =
    case it of
        Data.User.Bar ->
            --EOL

        Data.User.Baz ->
            --EOL

        Data.User.Qux ->
            --EOL
""")

    fun `test import with alias`() = checkFixByFileTree("""
--@ Data/User.elm
module Data.User exposing (..)
type Foo = Bar | Baz | Qux

--@ main.elm
import Data.User as UserData
foo : UserData.Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
import Data.User as UserData
foo : UserData.Foo -> ()
foo it =
    case it of
        UserData.Bar ->
            --EOL

        UserData.Baz ->
            --EOL

        UserData.Qux ->
            --EOL
""")

    fun `test exposed import`() = checkFixByFileTree("""
--@ Data/User.elm
module Data.User exposing (..)
type Foo = Bar | Baz | Qux

--@ main.elm
import Data.User exposing (..)
foo : Data.User.Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
import Data.User exposing (..)
foo : Data.User.Foo -> ()
foo it =
    case it of
        Bar ->
            --EOL

        Baz ->
            --EOL

        Qux ->
            --EOL
""")

    private fun checkFixByText(
            @Language("Elm") before: String,
            @Language("Elm") after: String) {
        // We use the --EOL marker to avoid editors trimming trailing whitespace, which is
        // significant for this test.
        checkFixByText("Add missing case branches", before, after.replace("--EOL", ""))
    }

    private fun checkFixByFileTree(
            @Language("Elm") before: String,
            @Language("Elm") after: String) {
        // We use the --EOL marker to avoid editors trimming trailing whitespace, which is
        // significant for this test.
        checkFixByFileTree("Add missing case branches", before, after.replace("--EOL", ""))
    }
}
