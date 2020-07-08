package org.elm.ide.intentions


class RemovePipelineIntentionTest : ElmIntentionTestBase(RemovePipelineIntention()) {


    fun `test right pipe to no pipe`() = doAvailableTest(
            """
module Foo exposing (list)

list = [1, 2, 3, 4] |> List.ma{-caret-}p times2

times2 m n = m * n
""", """
module Foo exposing (list)

list = (List.map times2 ([1, 2, 3, 4]))

times2 m n = m * n
""")


    fun `test remove pipeline example from elm-spa`() = doAvailableTest(
            """
module Foo exposing (value)

value =
    Editor.i{-caret-}nitEdit session slug
        |> updateWith (Editor (Just slug)) GotEditorMsg model
""", """
module Foo exposing (value)

value =
    (updateWith (Editor (Just slug)) GotEditorMsg model (Editor.initEdit session slug))
""")

    fun `test remove pipeline example 2 from elm-spa`() = doAvailableTest(
            """
module Foo exposing (value)

value msg =
    ( msg, True )
        |> Decode.succeed
        |> stop{-caret-}PropagationOn "click"
""", """
module Foo exposing (value)

value msg =
    (stopPropagationOn "click" (Decode.succeed (( msg, True ))))
""")

    fun `test remove pipeline example 3 from elm-spa`() = doAvailableTest(
            """
module Foo exposing (toggleFavoriteButton)

toggleFavoriteButton classStr msg attrs kids =
    i [ class "ion-heart" ] []
        :: kids
        |> Html.but{-caret-}ton (class classStr :: onClickStopPropagation msg :: attrs)
""", """
module Foo exposing (toggleFavoriteButton)

toggleFavoriteButton classStr msg attrs kids =
    (Html.button (class classStr :: onClickStopPropagation msg :: attrs) (i [ class "ion-heart" ] [] :: kids))
""")

    fun `test remove left pipeline`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    flo{-caret-}or <| 1.3 + 4
""", """
module Foo exposing (example)

example =
    (floor (1.3 + 4))
""")

}
