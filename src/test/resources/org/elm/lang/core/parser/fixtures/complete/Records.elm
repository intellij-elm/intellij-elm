type alias Point =
    { x : Int
    , y : Int
    }


p =
    { x = 0
    , y = 0
    }


emptyRecord =
    {}


makePair : { a | x : Int, y : Int } -> (Int, Int)
makePair { x, y } =
    ( x, y )

input =  { x = \() -> () }
valueRef = { foo = input.x (), bar = () }
