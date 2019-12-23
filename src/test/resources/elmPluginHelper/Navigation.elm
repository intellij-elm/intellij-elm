suite1 : Test
suite1 =
    describe "suite1"
        [ test "test1" <|
            \_ -> Expect.pass
        ]

test1 : Test
test1 =
    test "test1" <|
        \_ -> Expect.pass

suite2 : Test
suite2 =
    describe "suite2"
        [ test "test1" <|
            \_ -> Expect.pass
        , describe "nested1"
            [ test "test1" <|
                \_ -> Expect.pass
            , fuzz fuzzer "fuzz1" <|
                \_ -> Expect.pass
            ]
        ]

fuzz1 : Test
fuzz1 =
    fuzz fuzzer "fuzz1" <|
        \_ -> Expect.pass
