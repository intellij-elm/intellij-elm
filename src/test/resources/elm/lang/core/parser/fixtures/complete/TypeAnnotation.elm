update : Msg -> Foo.Model -> (Foo.Model, Cmd Msg)

map : (a -> b) -> List a -> List b

titleOfThing : { a | title : String } -> String

second : (a, b) -> b