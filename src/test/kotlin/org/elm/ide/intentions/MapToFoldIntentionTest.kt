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

}