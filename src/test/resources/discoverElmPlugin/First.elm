suite : Test
suite =
    describe "describe1"
        [ test "test1" <|
            \_ -> Expect.pass
        , test "test2" <|
                \_ -> Expect.fail "boom"
        ]
