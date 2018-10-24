f x =
    case x of
        Just xx ->
            xx

        Nothing ->
            0

        Foo Bar baz ->
            baz

g x y =
    case (x,y) of
        (0, 0) ->
            "origin"

        (0, y2) ->
            "x-axis"

        (x2, 0) ->
            "y-axis"

        _ ->
            "somewhere else"

h list =
  case list of
    [] -> 1
    (single :: []) :: [] -> 2
    [first, second] -> 3
    _ :: b -> 4

i record =
  case record of
    {x,y} -> ()
