package org.elm.ide.intentions

class MakeDecoderIntentionTest : ElmIntentionTestBase(MakeDecoderIntention()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test unavailable with wrong return type`() = doUnavailableTest(
            """
decode : String -> String{-caret-}
""")

    fun `test unavailable with arguments`() = doUnavailableTest(
            """
import Json.Decode as Decode

decode : Int -> Decode Int{-caret-}
""")

    fun `test basic type`() = doAvailableTest(
            """
import Json.Decode as Decode

decode : Decode.Decoder String{-caret-}
""", """
import Json.Decode as Decode

decode : Decode.Decoder String
decode =
    Decode.string
""")

    fun `test existing pipeline import`() = doAvailableTest(
            """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type alias Foo = { foo : Int }

decode : Decode.Decoder Foo{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type alias Foo = { foo : Int }

decode : Decode.Decoder Foo
decode =
    Decode.succeed Foo
        |> required "foo" Decode.int
""")

    fun `test pipeline exposed`() = doAvailableTest(
            """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (..)

type alias Foo = { foo : Int }

decode : Decode.Decoder Foo{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (..)

type alias Foo = { foo : Int }

decode : Decode.Decoder Foo
decode =
    Decode.succeed Foo
        |> required "foo" Decode.int
""")

    fun `test exposed functions`() = doAvailableTest(
            """
import Json.Decode exposing (..)
import Maybe exposing (..)
import String exposing (..)

type alias Foo = { s : Maybe String}

decode : Decoder Foo{-caret-}
""", """
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required)
import Maybe exposing (..)
import String exposing (..)

type alias Foo = { s : Maybe String}

decode : Decoder Foo
decode =
    succeed Foo
        |> required "s" (nullable string)
""")

    fun `test aliased functions`() = doAvailableTest(
            """
import Json.Decode as D
import Maybe as M
import String as S

type alias Foo = { s : M.Maybe S.String}

decode : D.Decoder Foo{-caret-}
""", """
import Json.Decode as D
import Json.Decode.Pipeline exposing (required)
import Maybe as M
import String as S

type alias Foo = { s : M.Maybe S.String}

decode : D.Decoder Foo
decode =
    D.succeed Foo
        |> required "s" (D.nullable D.string)
""")

    fun `test built in types in record`() = doAvailableTest(
            """
import Array exposing (Array)
import Dict exposing (Dict)
import Json.Decode as Decode
import Set exposing (Set)

type alias Foo =
    { intField : Int
    , floatField : Float
    , boolField : Bool
    , stringField : String
    , charField : Char
    , listIntField : List Int
    , maybeStringField : Maybe String
    , dictField : Dict String Float
    , arrayField : Array String
    , setField : Set Int
    , tuple2Field : ( Bool, String )
    , tuple3Field : ( Int, String, Float )
    , unitField : ()
    }


decode{-caret-} : Decode.Decoder Foo
""", """
import Array exposing (Array)
import Dict exposing (Dict)
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)
import Set exposing (Set)

type alias Foo =
    { intField : Int
    , floatField : Float
    , boolField : Bool
    , stringField : String
    , charField : Char
    , listIntField : List Int
    , maybeStringField : Maybe String
    , dictField : Dict String Float
    , arrayField : Array String
    , setField : Set Int
    , tuple2Field : ( Bool, String )
    , tuple3Field : ( Int, String, Float )
    , unitField : ()
    }


decode : Decode.Decoder Foo
decode =
    Decode.succeed Foo
        |> required "intField" Decode.int
        |> required "floatField" Decode.float
        |> required "boolField" Decode.bool
        |> required "stringField" Decode.string
        |> required "charField" (Decode.string |> Decode.map (String.toList >> List.head >> Maybe.withDefault '?'))
        |> required "listIntField" (Decode.list Decode.int)
        |> required "maybeStringField" (Decode.nullable Decode.string)
        |> required "dictField" (Decode.dict Decode.float)
        |> required "arrayField" (Decode.array Decode.string)
        |> required "setField" (Decode.map Set.fromList (Decode.list Decode.int))
        |> required "tuple2Field" (Decode.map2 Tuple.pair (Decode.index 0 Decode.bool) (Decode.index 1 Decode.string))
        |> required "tuple3Field" (Decode.map3 (\a b c -> ( a, b, c )) (Decode.index 0 Decode.int) (Decode.index 1 Decode.string) (Decode.index 2 Decode.float))
        |> required "unitField" (Decode.succeed ())
""")

    fun `test mixed records and unions`() = doAvailableTest(
            """
module Main exposing (..)
import Json.Decode as Decode

type Enum = Baz | Qux
type alias Foo = { foo1 : String, foo2 : Int, enum : Enum }
type alias Bar = { bar1 : String, fooRef : Foo }

f : Decode.Decoder Bar{-caret-}
""", """
module Main exposing (..)
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type Enum = Baz | Qux
type alias Foo = { foo1 : String, foo2 : Int, enum : Enum }
type alias Bar = { bar1 : String, fooRef : Foo }

f : Decode.Decoder Bar
f =
    Decode.succeed Bar
        |> required "bar1" Decode.string
        |> required "fooRef" fooDecoder


-- TODO: double-check generated code
enumDecoder : Decode.Decoder Enum
enumDecoder =
    let
        get id =
            case id of
                "Baz" ->
                    Decode.succeed Baz

                "Qux" ->
                    Decode.succeed Qux

                _ ->
                    Decode.fail ("unknown value for Enum: " ++ id)
    in
    Decode.string |> Decode.andThen get


-- TODO: double-check generated code
fooDecoder : Decode.Decoder Foo
fooDecoder =
    Decode.succeed Foo
        |> required "foo1" Decode.string
        |> required "foo2" Decode.int
        |> required "enum" enumDecoder
""")

    fun `test nested function`() = doAvailableTest(
            """
module Main exposing (..)
import Json.Decode as Decode

type Enum = Baz | Qux
type alias Foo = { enum : Enum }

main =
    let
        decode : Decode.Decoder Foo{-caret-}
    in
    ()
""", """
module Main exposing (..)
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type Enum = Baz | Qux
type alias Foo = { enum : Enum }

main =
    let
        decode : Decode.Decoder Foo        
        decode =
            Decode.succeed Foo
                |> required "enum" enumDecoder


        -- TODO: double-check generated code
        enumDecoder : Decode.Decoder Enum
        enumDecoder =
            let
                get id =
                    case id of
                        "Baz" ->
                            Decode.succeed Baz

                        "Qux" ->
                            Decode.succeed Qux

                        _ ->
                            Decode.fail ("unknown value for Enum: " ++ id)
            in
            Decode.string |> Decode.andThen get
    in
    ()
""")

    fun `test union variant wrappers`() = doAvailableTest(
            """
import Json.Decode as Decode

type UUID = UUID String
type Wrappers = Bar String | Baz Int | Qux
type alias Foo = { uuid: UUID, wrappers : Wrappers }

decode : Decode.Decoder Foo{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type UUID = UUID String
type Wrappers = Bar String | Baz Int | Qux
type alias Foo = { uuid: UUID, wrappers : Wrappers }

decode : Decode.Decoder Foo
decode =
    Decode.succeed Foo
        |> required "uuid" uuidDecoder
        |> required "wrappers" wrappersDecoder


-- TODO: double-check generated code
uuidDecoder : Decode.Decoder UUID
uuidDecoder =
    Decode.map UUID Decode.string


-- TODO: double-check generated code
wrappersDecoder : Decode.Decoder Wrappers
wrappersDecoder =
    let
        get id =
            case id of
                "Bar" ->
                    Debug.todo "Cannot decode variant with params: Bar"

                "Baz" ->
                    Debug.todo "Cannot decode variant with params: Baz"

                "Qux" ->
                    Decode.succeed Qux

                _ ->
                    Decode.fail ("unknown value for Wrappers: " ++ id)
    in
    Decode.string |> Decode.andThen get
""")

    fun `test union variants with multiple parameters`() = doAvailableTest(
            """
import Json.Decode as Decode

type Foo a = Bar String Int | Baz a a a

decode : Decode.Decoder (Foo Int){-caret-}
""", """
import Json.Decode as Decode

type Foo a = Bar String Int | Baz a a a

decode : Decode.Decoder (Foo Int)
decode =
    let
        get id =
            case id of
                "Bar" ->
                    Debug.todo "Cannot decode variant with params: Bar"

                "Baz" ->
                    Debug.todo "Cannot decode variant with params: Baz"

                _ ->
                    Decode.fail ("unknown value for Foo Int: " ++ id)
    in
    Decode.string |> Decode.andThen get
""")

    fun `test name conflict`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Decode as Decode
import Foo

type alias Bar = { f : Foo.Bar }
type alias Model = { bar : Bar }

decode : Decode.Decoder Model{-caret-}
--@ Foo.elm
module Foo exposing (Bar)
type alias Bar = { s : String }
""", """
import Json.Decode as Decode
import Foo
import Json.Decode.Pipeline exposing (required)

type alias Bar = { f : Foo.Bar }
type alias Model = { bar : Bar }

decode : Decode.Decoder Model
decode =
    Decode.succeed Model
        |> required "bar" barDecoder


-- TODO: double-check generated code
fooBarDecoder : Decode.Decoder Foo.Bar
fooBarDecoder =
    Decode.succeed Foo.Bar
        |> required "s" Decode.string


-- TODO: double-check generated code
barDecoder : Decode.Decoder Bar
barDecoder =
    Decode.succeed Bar
        |> required "f" fooBarDecoder
""")

    fun `test adding import`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Decode as Decode
import Foo exposing (Foo)

decode : Decode.Decoder Foo{-caret-}
--@ Foo.elm
module Foo exposing (..)
import Bar

type alias Foo = { bar : Bar.Bar }
--@ Bar.elm
module Bar exposing (..)

type alias Bar = { s : String }
""", """
import Bar
import Json.Decode as Decode
import Foo exposing (Foo)
import Json.Decode.Pipeline exposing (required)

decode : Decode.Decoder Foo
decode =
    Decode.succeed Foo
        |> required "bar" barDecoder


-- TODO: double-check generated code
barDecoder : Decode.Decoder Bar.Bar
barDecoder =
    Decode.succeed Bar.Bar
        |> required "s" Decode.string
""")

    fun `test adding variant imports`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Decode as Decode
import Foo

type alias Baz = { foo : Foo.Foo, bar : Foo.Bar }

decode : Decode.Decoder Baz{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Foo = Foo String
type Bar = Baz | Qux
""", """
import Json.Decode as Decode
import Foo exposing (Bar(..), Foo(..))
import Json.Decode.Pipeline exposing (required)

type alias Baz = { foo : Foo.Foo, bar : Foo.Bar }

decode : Decode.Decoder Baz
decode =
    Decode.succeed Baz
        |> required "foo" fooDecoder
        |> required "bar" barDecoder


-- TODO: double-check generated code
fooDecoder : Decode.Decoder Foo.Foo
fooDecoder =
    Decode.map Foo Decode.string


-- TODO: double-check generated code
barDecoder : Decode.Decoder Foo.Bar
barDecoder =
    let
        get id =
            case id of
                "Baz" ->
                    Decode.succeed Foo.Baz

                "Qux" ->
                    Decode.succeed Foo.Qux

                _ ->
                    Decode.fail ("unknown value for Bar: " ++ id)
    in
    Decode.string |> Decode.andThen get
""")

    fun `test existing union decoder`() = doAvailableTest(
            """
import Json.Decode as Decode

type Foo = Foo
type alias Bar = { foo : Foo }

existing : Decode.Decoder Foo
existing = Decode.succeed Foo

decode : Decode.Decoder Bar{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type Foo = Foo
type alias Bar = { foo : Foo }

existing : Decode.Decoder Foo
existing = Decode.succeed Foo

decode : Decode.Decoder Bar
decode =
    Decode.succeed Bar
        |> required "foo" existing
""")

    fun `test existing record decoder`() = doAvailableTest(
            """
import Json.Decode as Decode

type alias Foo = { s : String }
type alias Bar = { foo : Foo }

existing : Decode.Decoder Foo
existing = Decode.succeed { s = "" }

decode : Decode.Decoder Bar{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type alias Foo = { s : String }
type alias Bar = { foo : Foo }

existing : Decode.Decoder Foo
existing = Decode.succeed { s = "" }

decode : Decode.Decoder Bar
decode =
    Decode.succeed Bar
        |> required "foo" existing
""")

    fun `test existing list decoder`() = doAvailableTest(
            """
import Json.Decode as Decode

type Foo = Foo
type alias Bar = { foo : List Foo }

existing : Decode.Decoder (List Foo)
existing = Decode.succeed []

decode : Decode.Decoder Bar{-caret-}
""", """
import Json.Decode as Decode
import Json.Decode.Pipeline exposing (required)

type Foo = Foo
type alias Bar = { foo : List Foo }

existing : Decode.Decoder (List Foo)
existing = Decode.succeed []

decode : Decode.Decoder Bar
decode =
    Decode.succeed Bar
        |> required "foo" existing
""")

    fun `test existing union decoder in other module`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Decode as Decode
import Foo

type alias Bar = { foo : Foo.Foo }

decode : Decode.Decoder Bar{-caret-}
--@ Foo.elm
module Foo exposing (..)
import Json.Decode as Decode
type Foo = Foo
existing : Decode.Decoder Foo
existing = Decode.succeed Foo
""", """
import Json.Decode as Decode
import Foo
import Json.Decode.Pipeline exposing (required)

type alias Bar = { foo : Foo.Foo }

decode : Decode.Decoder Bar
decode =
    Decode.succeed Bar
        |> required "foo" Foo.existing
""")

    fun `test existing union decoder in separate module with qualification`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Decode as Decode
import Foo
import Bar

decode : Decode.Decoder Foo.Alias{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Type = Type
type alias Alias = { t : Type }
--@ Bar.elm
module Bar exposing (..)
import Foo exposing (..)
import Json.Decode as Decode
existing : Decode.Decoder Type
existing = Decode.succeed Type
""", """
import Json.Decode as Decode
import Foo
import Bar
import Json.Decode.Pipeline exposing (required)

decode : Decode.Decoder Foo.Alias
decode =
    Decode.succeed Foo.Alias
        |> required "t" Bar.existing
""")
}
