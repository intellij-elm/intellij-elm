module Main exposing (main)

import Foo exposing (Foo, bar, Baz(A,B))

i : Foo
i = Foo.z.n

main =
    let
        a = A
        x = bar
    in
        a x
