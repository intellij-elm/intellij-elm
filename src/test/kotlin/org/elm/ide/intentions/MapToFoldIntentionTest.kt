package org.elm.ide.intentions


class MapToFoldIntentionTest : ElmIntentionTestBase(MapToFoldIntention()) {


    fun `test converts to fold`() = doAvailableTest(
            """
module Foo exposing (f0)
f0 = 
    List.m{-caret-}ap f items
""", """
module Foo exposing (f0)
f0 = 
    List.foldr (\item result -> f item :: result) [] items
""")


    fun `test transforms functions with case statements`() = doAvailableTest(
            """
module Foo exposing (f0)

greet greetWord name =
    greetWord ++ ", " ++ name ++ "!"

f0 = 
    List.ma{-caret-}p
        (greet
            (case thing of
                Hello ->
                    "Hello"

                Hi ->
                    "Hi"
            )
        )
        names
""", """
module Foo exposing (f0)

greet greetWord name =
    greetWord ++ ", " ++ name ++ "!"

f0 = 
    List.foldr
        (\item result ->
            greet
                (case thing of
                    Hello ->
                        "Hello"

                    Hi ->
                        "Hi"
                )
                item
                :: result
        )
        []
        names
""")

}