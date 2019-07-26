package org.elm.ide.intentions

class MakeEncoderIntentionTest : ElmIntentionTestBase(MakeEncoderIntention()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test basic type`() = doAvailableTest(
            """
import Json.Encode as Encode

encode : String -> Encode.Value{-caret-}
"""
            , """
import Json.Encode as Encode

encode : String -> Encode.Value{-caret-}
encode string =
    Encode.string string
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
    , setField : Set Bool
    , tuple2Field : ( Int, String )
    , tuple3Field : ( Int, String, Float )
    , unitField : ()
    }


foo{-caret-} : Foo -> Encode.Value
"""
            , """
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
    , setField : Set Bool
    , tuple2Field : ( Int, String )
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
        , ( "setField", Encode.set Encode.bool foo.setField )
        , ( "tuple2Field", (\( a, b ) -> Encode.list identity [ Encode.int a, Encode.string b ]) foo.tuple2Field )
        , ( "tuple3Field", (\( a, b, c ) -> Encode.list identity [ Encode.int a, Encode.string b, Encode.float c ]) foo.tuple3Field )
        , ( "unitField", (\_ -> Encode.null) foo.unitField )
        ]
""")

    fun `test mixed records and unions`() = doAvailableTest(
            """
import Json.Encode as Encode
type Enum = Baz | Qux
type alias Foo = { foo1 : String, foo2 : Int, enum : Enum }
type alias Bar = { bar1 : String, fooRef : Foo }

f : Bar -> Encode.Value{-caret-}
"""
            , """
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


encodeEnum : Enum -> Encode.Value
encodeEnum enum =
    case enum of
        Baz ->
            Encode.string "Baz"

        Qux ->
            Encode.string "Qux"


encodeFoo : Foo -> Encode.Value
encodeFoo foo =
    Encode.object <|
        [ ( "foo1", Encode.string foo.foo1 )
        , ( "foo2", Encode.int foo.foo2 )
        , ( "enum", encodeEnum foo.enum )
        ]
""")
}
