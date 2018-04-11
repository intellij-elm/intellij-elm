f = case x of
        0 -> "zero"
        _ -> "other"

g = case f x of
        "zero" -> 0
        _ -> case x of
                0 -> 1
                _ -> 42

h = case f x of
        "other" ->
            case x of
                0 -> 1
                _ -> 42
        "zero" -> 0
        _ ->
            99
