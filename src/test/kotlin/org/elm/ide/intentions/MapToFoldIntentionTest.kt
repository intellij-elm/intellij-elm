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
            (greet
                (case thing of
                    Hello ->
                        "Hello"

                    Hi ->
                        "Hi"
                )
            )
                item
                :: result
        )
        []
        names
""")

    fun `test not available for functions besides map`() = doUnavailableTest(
            """
module Foo exposing (f0)
f0 = 
    List.filt{-caret-}er f items
""")


    fun `test not available when inner func call is not map but outer func call is map`() = doUnavailableTest(
            """
module Foo exposing (f0)
f0 = 
    List.map (\item -> List.singleton{-caret-} item) []
""")


    fun `test piped function`() = doAvailableTest(
            """
module Foo exposing (f0)
f0 = 
    items
    |> List.m{-caret-}ap f
""", """
module Foo exposing (f0)
f0 = 
    items
    |> List.foldr (\item result -> f item :: result) []
""")


    fun `test preserves indentation from surrounding context`() = doAvailableTest(
            """
module Foo exposing (view)

view : Model -> { title : String, content : Html Msg }
view model =
    { title = "Conduit"
    , content =
        div [ class "home-page" ]
            [ viewBanner
            , div [ class "container page" ]
                [ [ case model.feed of
                        Loaded feed ->
                                [ div [ class "feed-toggle" ] <|
                                    List.concat
                                        [ [ viewTabs
                                                (Session.cred model.session)
                                                model.feedTab
                                          ]
                                        , List.m{-caret-}ap
                                            (Html.map
                                                GotFeedMsg
                                            )
                                            (Feed.viewArticles model.timeZone
                                                feed
                                            )
                                        , [ Feed.viewPagination ClickedFeedPage model.feedPage feed ]
                                        ]
                                ]

                        Loading ->
                            []
""", """
module Foo exposing (view)

view : Model -> { title : String, content : Html Msg }
view model =
    { title = "Conduit"
    , content =
        div [ class "home-page" ]
            [ viewBanner
            , div [ class "container page" ]
                [ [ case model.feed of
                        Loaded feed ->
                                [ div [ class "feed-toggle" ] <|
                                    List.concat
                                        [ [ viewTabs
                                                (Session.cred model.session)
                                                model.feedTab
                                          ]
                                        , List.foldr
                                            (\item result ->
                                                (Html.map
                                                    GotFeedMsg
                                                )
                                                    item
                                                    :: result
                                            )
                                            []
                                            (Feed.viewArticles model.timeZone
                                                feed
                                            )
                                        , [ Feed.viewPagination ClickedFeedPage model.feedPage feed ]
                                        ]
                                ]

                        Loading ->
                            []
""")

    fun `test introduces unique parameter names`() = doAvailableTest(
            """
module Foo exposing (f0)

item = ()
item1 = ()
result = ()
result1 = ()

f0 = 
    List.m{-caret-}ap f items
""", """
module Foo exposing (f0)

item = ()
item1 = ()
result = ()
result1 = ()

f0 = 
    List.foldr (\item2 result2 -> f item2 :: result2) [] items
""")


}