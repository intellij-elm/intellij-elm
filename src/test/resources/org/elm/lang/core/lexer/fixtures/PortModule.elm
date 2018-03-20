port module Ports exposing (f, g)

import Json.Encode exposing (Value)

port f : String -> Cmd msg

port g : (Value -> msg) -> Sub msg
