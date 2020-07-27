package org.elm.ide.intentions


class MapToFoldIntentionTest : ElmIntentionTestBase(MapToFoldIntention()) {


    fun `test expose a function`() = doAvailableTest(
            """
module Foo exposing (f0)
f0 = 
    List.m{-caret-}ap f items
""", """
module Foo exposing (f0)
f0 = 
    List.foldr (\item result -> f item :: result) [] items
""")

}