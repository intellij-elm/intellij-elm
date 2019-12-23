package org.elm.ide.intentions

class MakeEncoderIntentionTest : ElmIntentionTestBase(MakeEncoderIntention()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test unavailable with wrong return type`() = doUnavailableTest(
            """
encode : String -> String{-caret-}
""")

    fun `test unavailable with no arguments`() = doUnavailableTest(
            """
import Json.Encode as Encode

encode : Encode.Value{-caret-}
""")

    fun `test unavailable with too many arguments`() = doUnavailableTest(
            """
import Json.Encode as Encode

encode : String -> String -> Encode.Value{-caret-}
""")

    fun `test basic type`() = doAvailableTest(
            """
import Json.Encode as Encode

encode : String -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

encode : String -> Encode.Value
encode string =
    Encode.string string
""")

    fun `test exposed functions`() = doAvailableTest(
            """
import Json.Encode exposing (..)
import Maybe exposing (..)
import String exposing (..)

type alias Foo = { s : Maybe String}

encode : Foo -> Value{-caret-}
""", """
import Json.Encode exposing (..)
import Maybe exposing (..)
import String exposing (..)

type alias Foo = { s : Maybe String}

encode : Foo -> Value
encode foo =
    object <|
        [ ( "s", (map string >> withDefault null) foo.s )
        ]
""")

    fun `test aliased functions`() = doAvailableTest(
            """
import Json.Encode as E
import Maybe as M
import String as S

type alias Foo = { s : M.Maybe S.String}

encode : Foo -> E.Value{-caret-}
""", """
import Json.Encode as E
import Maybe as M
import String as S

type alias Foo = { s : M.Maybe S.String}

encode : Foo -> E.Value
encode foo =
    E.object <|
        [ ( "s", (M.map E.string >> M.withDefault E.null) foo.s )
        ]
""")

    fun `test built in types in record`() = doAvailableTest(
            """
import Json.Encode as Encode
import Dict exposing (Dict)
import Set exposing (Set)
import Array exposing (Array)

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


foo{-caret-} : Foo -> Encode.Value
""", """
import Json.Encode as Encode
import Dict exposing (Dict)
import Set exposing (Set)
import Array exposing (Array)

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


foo : Foo -> Encode.Value
foo foo =
    Encode.object <|
        [ ( "intField", Encode.int foo.intField )
        , ( "floatField", Encode.float foo.floatField )
        , ( "boolField", Encode.bool foo.boolField )
        , ( "stringField", Encode.string foo.stringField )
        , ( "charField", (String.fromChar >> Encode.string) foo.charField )
        , ( "listIntField", Encode.list Encode.int foo.listIntField )
        , ( "maybeStringField", (Maybe.map Encode.string >> Maybe.withDefault Encode.null) foo.maybeStringField )
        , ( "dictField", (Dict.toList >> List.map (\( k, v ) -> ( k, Encode.float v )) >> Encode.object) foo.dictField )
        , ( "arrayField", Encode.array Encode.string foo.arrayField )
        , ( "setField", Encode.set Encode.int foo.setField )
        , ( "tuple2Field", (\( a, b ) -> Encode.list identity [ Encode.bool a, Encode.string b ]) foo.tuple2Field )
        , ( "tuple3Field", (\( a, b, c ) -> Encode.list identity [ Encode.int a, Encode.string b, Encode.float c ]) foo.tuple3Field )
        , ( "unitField", (\_ -> Encode.null) foo.unitField )
        ]
""")

    fun `test mixed records and unions`() = doAvailableTest(
            """
module Main exposing (..)
import Json.Encode as Encode

type Enum = Baz | Qux
type alias Foo = { foo1 : String, foo2 : Int, enum : Enum }
type alias Bar = { bar1 : String, fooRef : Foo }

f : Bar -> Encode.Value{-caret-}
""", """
module Main exposing (..)
import Json.Encode as Encode

type Enum = Baz | Qux
type alias Foo = { foo1 : String, foo2 : Int, enum : Enum }
type alias Bar = { bar1 : String, fooRef : Foo }

f : Bar -> Encode.Value
f bar =
    Encode.object <|
        [ ( "bar1", Encode.string bar.bar1 )
        , ( "fooRef", encodeFoo bar.fooRef )
        ]


-- TODO: double-check generated code
encodeEnum : Enum -> Encode.Value
encodeEnum enum =
    case enum of
        Baz ->
            Encode.string "Baz"

        Qux ->
            Encode.string "Qux"


-- TODO: double-check generated code
encodeFoo : Foo -> Encode.Value
encodeFoo foo =
    Encode.object <|
        [ ( "foo1", Encode.string foo.foo1 )
        , ( "foo2", Encode.int foo.foo2 )
        , ( "enum", encodeEnum foo.enum )
        ]
""")

    fun `test nested function`() = doAvailableTest(
            """
import Json.Encode as Encode

type Enum = Baz | Qux
type alias Foo = { enum : Enum }

main =
    let
        encode : Foo -> Encode.Value{-caret-}
    in
    ()
""", """
import Json.Encode as Encode

type Enum = Baz | Qux
type alias Foo = { enum : Enum }

main =
    let
        encode : Foo -> Encode.Value        
        encode foo =
            Encode.object <|
                [ ( "enum", encodeEnum foo.enum )
                ]


        -- TODO: double-check generated code
        encodeEnum : Enum -> Encode.Value
        encodeEnum enum =
            case enum of
                Baz ->
                    Encode.string "Baz"

                Qux ->
                    Encode.string "Qux"
    in
    ()
""")

    fun `test union variant wrappers`() = doAvailableTest(
            """
import Json.Encode as Encode

type UUID = UUID String
type Wrappers = Bar String | Baz Int | Qux
type alias Foo = { uuid: UUID, wrappers : Wrappers }

encode : Foo -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

type UUID = UUID String
type Wrappers = Bar String | Baz Int | Qux
type alias Foo = { uuid: UUID, wrappers : Wrappers }

encode : Foo -> Encode.Value
encode foo =
    Encode.object <|
        [ ( "uuid", encodeUUID foo.uuid )
        , ( "wrappers", encodeWrappers foo.wrappers )
        ]


-- TODO: double-check generated code
encodeUUID : UUID -> Encode.Value
encodeUUID (UUID string) =
    Encode.string string


-- TODO: double-check generated code
encodeWrappers : Wrappers -> Encode.Value
encodeWrappers wrappers =
    case wrappers of
        Bar string ->
            Encode.string string

        Baz int ->
            Encode.int int

        Qux ->
            Encode.string "Qux"
""")

    fun `test union variants with multiple parameters`() = doAvailableTest(
            """
import Json.Encode as Encode

type Foo a = Bar String Int | Baz a a a 

encode : Foo Int -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

type Foo a = Bar String Int | Baz a a a 

encode : Foo Int -> Encode.Value
encode foo =
    case foo of
        Bar string int ->
            Debug.todo "Cannot generate encoder for variant with multiple parameters"

        Baz a a a ->
            Debug.todo "Cannot generate encoder for variant with multiple parameters"
""")

    fun `test name conflict`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Encode as Encode
import Foo

type alias Bar = { f : Foo.Bar } 
type alias Model = { bar : Bar }

encode : Model -> Encode.Value{-caret-}
--@ Foo.elm
module Foo exposing (Bar)
type alias Bar = { s : String }
""", """
import Json.Encode as Encode
import Foo

type alias Bar = { f : Foo.Bar } 
type alias Model = { bar : Bar }

encode : Model -> Encode.Value
encode model =
    Encode.object <|
        [ ( "bar", encodeBar model.bar )
        ]


-- TODO: double-check generated code
encodeFooBar : Foo.Bar -> Encode.Value
encodeFooBar bar =
    Encode.object <|
        [ ( "s", Encode.string bar.s )
        ]


-- TODO: double-check generated code
encodeBar : Bar -> Encode.Value
encodeBar bar =
    Encode.object <|
        [ ( "f", encodeFooBar bar.f )
        ]
""")

    fun `test adding import`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Encode as Encode
import Foo exposing (Foo)

encode : Foo -> Encode.Value{-caret-}
--@ Foo.elm
module Foo exposing (..)
import Bar
type alias Foo = { bar : Bar.Bar }
--@ Bar.elm
module Bar exposing (..)
type alias Bar = { s : String }
""", """
import Bar
import Json.Encode as Encode
import Foo exposing (Foo)

encode : Foo -> Encode.Value
encode foo =
    Encode.object <|
        [ ( "bar", encodeBar foo.bar )
        ]


-- TODO: double-check generated code
encodeBar : Bar.Bar -> Encode.Value
encodeBar bar =
    Encode.object <|
        [ ( "s", Encode.string bar.s )
        ]
""")

    fun `test adding variant imports`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Encode as Encode
import Foo

type alias Baz = { foo : Foo.Foo, bar : Foo.Bar }

encode : Baz -> Encode.Value{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Foo = Foo String
type Bar = Baz | Qux
""", """
import Json.Encode as Encode
import Foo exposing (Bar(..), Foo(..))

type alias Baz = { foo : Foo.Foo, bar : Foo.Bar }

encode : Baz -> Encode.Value
encode baz =
    Encode.object <|
        [ ( "foo", encodeFoo baz.foo )
        , ( "bar", encodeBar baz.bar )
        ]


-- TODO: double-check generated code
encodeFoo : Foo.Foo -> Encode.Value
encodeFoo (Foo string) =
    Encode.string string


-- TODO: double-check generated code
encodeBar : Foo.Bar -> Encode.Value
encodeBar bar =
    case bar of
        Baz ->
            Encode.string "Baz"

        Qux ->
            Encode.string "Qux"
""")

    fun `test existing union encoder`() = doAvailableTest(
            """
import Json.Encode as Encode

type Foo = Foo
type alias Bar = { foo : Foo }

existing : Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

type Foo = Foo
type alias Bar = { foo : Foo }

existing : Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value
encode bar =
    Encode.object <|
        [ ( "foo", existing bar.foo )
        ]
""")

    fun `test existing record encoder`() = doAvailableTest(
            """
import Json.Encode as Encode

type alias Foo = { s : String }
type alias Bar = { foo : Foo }

existing : Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

type alias Foo = { s : String }
type alias Bar = { foo : Foo }

existing : Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value
encode bar =
    Encode.object <|
        [ ( "foo", existing bar.foo )
        ]
""")

    fun `test existing list encoder`() = doAvailableTest(
            """
import Json.Encode as Encode

type Foo = Foo
type alias Bar = { foo : List Foo }

existing : List Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value{-caret-}
""", """
import Json.Encode as Encode

type Foo = Foo
type alias Bar = { foo : List Foo }

existing : List Foo -> Encode.Value
existing foo = Encode.null

encode : Bar -> Encode.Value
encode bar =
    Encode.object <|
        [ ( "foo", existing bar.foo )
        ]
""")

    fun `test existing union encoder in other module`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Encode as Encode
import Foo

type alias Bar = { foo : Foo.Foo }

encode : Bar -> Encode.Value{-caret-}

--@ Foo.elm
module Foo exposing (..)
import Json.Encode as Encode
type Foo = Foo

existing : Foo -> Encode.Value
existing _ = Encode.null
""", """
import Json.Encode as Encode
import Foo

type alias Bar = { foo : Foo.Foo }

encode : Bar -> Encode.Value
encode bar =
    Encode.object <|
        [ ( "foo", Foo.existing bar.foo )
        ]

""")

    fun `test existing union encoder in separate module with qualification`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Json.Encode as Encode
import Foo
import Bar

encode : Foo.Alias -> Encode.Value{-caret-}
--@ Foo.elm
module Foo exposing (..)
type Type = Type
type alias Alias = { t : Type }
--@ Bar.elm
module Bar exposing (..)
import Foo exposing (..)
import Json.Encode as Encode
existing : Type -> Encode.Value
existing _ = Encode.null
""", """
import Json.Encode as Encode
import Foo
import Bar

encode : Foo.Alias -> Encode.Value
encode alias =
    Encode.object <|
        [ ( "t", Bar.existing alias.t )
        ]
""")
}
