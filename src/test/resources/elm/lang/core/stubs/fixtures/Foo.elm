module Foo exposing (..)

type alias Foo =
    { n : Int }

z : Foo
z = { n = 42 }

bar = "bar"

type Baz = A String | B
