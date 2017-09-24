f x =
    case x of
        Just xx ->
            xx

        Nothing ->
            0

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
