-- this is a line comment

n = 42 -- this is also a line comment


{- this is a block comment.
It can span multiple lines
-}
foo = 42

{- block comments can also be on a single line -}
bar = 99

{- block comments can be
    {- nested -}
like this
-}
baz = 0


{-| this is a doc comment.
It can span multiple lines
-}
foo2 = 42

{-| doc comments can also be on a single line -}
bar2 = 99

{-| doc comments can contain block comments
    {- nested -}
and everything is still ok
-}
baz2 = 0

