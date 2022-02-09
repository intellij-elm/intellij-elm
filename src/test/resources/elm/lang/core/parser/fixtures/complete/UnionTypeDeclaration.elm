type Msg
    = Dismiss
    | Show (Int, String)

type Configurator model
    = Init (() -> model)
    | Apply model Int (model -> Int -> model)
    | Configure { foo: Bool, bar: List model }
