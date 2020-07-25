package org.elm.ide.intentions


class RemovePipelineIntentionTest : ElmIntentionTestBase(RemovePipelineIntention()) {


    fun `test right pipe to no pipe`() = doAvailableTest(
            """
module Foo exposing (list)

list = [1, 2, 3, 4] |> List.ma{-caret-}p times2

times2 m n = m * n
""", """
module Foo exposing (list)

list = List.map times2 [1, 2, 3, 4]

times2 m n = m * n
""")


    fun `test preserves comments`() = doAvailableTest(
            """
module Foo exposing (list)

list = 
    -- comment 1
    [1, 2, 3, 4] {- comment 2 -}
        -- comment 3
        {- comment 4 -}
        |> List.ma{-caret-}p times2 -- comment 5
        -- comment 6
        |> identity -- comment 7

times2 m n = m * n
""", """
module Foo exposing (list)

list = 
    -- comment 1
    identity

    (List.map times2
-- comment 5
-- comment 6
    [1, 2, 3, 4]
    ) -- comment 7

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
    updateWith (Editor (Just slug)) GotEditorMsg model (Editor.initEdit session slug)
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
    stopPropagationOn "click" (Decode.succeed ( msg, True ))
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
    Html.button (class classStr :: onClickStopPropagation msg :: attrs) (i [ class "ion-heart" ] [] :: kids)
""")

    fun `test remove left pipeline`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    flo{-caret-}or <| 1.3 + 4
""", """
module Foo exposing (example)

example =
    floor (1.3 + 4)
""")


    fun `test keeps parens around lambdas`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    (\fn -> fn [])
        |> List.m{-caret-}ap
""", """
module Foo exposing (example)

example =
    List.map (\fn -> fn [])
""")

    fun `test paren is indented less than case statements to make valid whitespace`() = doAvailableTest(
            """
module Foo exposing (view)

view : Model -> { title : String, content : Html Msg }
view model =
    { title = "Conduit"
    , content =
        div
            [ "home-page"
                |> class
            ]
            [ viewBanner
            , div [ class "container page" ]
                [ div [ class "row" ]
                    [ div [ class "col-md-9" ] {-caret-}<|
                        case model.feed of
                            Loaded feed ->
                                [ div [ class "feed-toggle" ]
                                    (List.concat
                                        [ [ viewTabs
                                                (Session.cred model.session)
                                                model.feedTab
                                          ]
                                        , List.map (Html.map GotFeedMsg) (Feed.viewArticles model.timeZone feed)
                                        , [ Feed.viewPagination ClickedFeedPage model.feedPage feed ]
                                        ]
                                    )
                                ]

                            Loading ->
                                []

                            LoadingSlowly ->
                                [ Loading.icon ]

                            Failed ->
                                [ Loading.error "feed" ]
                    , div [ class "col-md-3" ] <|
                        case model.tags of
                            Loaded tags ->
                                [ div [ class "sidebar" ] <|
                                    [ p [] [ text "Popular Tags" ]
                                    , viewTags tags
                                    ]
                                ]

                            Loading ->
                                []

                            LoadingSlowly ->
                                [ Loading.icon ]

                            Failed ->
                                [ Loading.error "tags" ]
                    ]
                ]
            ]
    }
""", """
module Foo exposing (view)

view : Model -> { title : String, content : Html Msg }
view model =
    { title = "Conduit"
    , content =
        div
            [ "home-page"
                |> class
            ]
            [ viewBanner
            , div [ class "container page" ]
                [ div [ class "row" ]
                    [ div [ class "col-md-9" ] (case model.feed of
                            Loaded feed ->
                                [ div [ class "feed-toggle" ]
                                    (List.concat
                                        [ [ viewTabs
                                                (Session.cred model.session)
                                                model.feedTab
                                          ]
                                        , List.map (Html.map GotFeedMsg) (Feed.viewArticles model.timeZone feed)
                                        , [ Feed.viewPagination ClickedFeedPage model.feedPage feed ]
                                        ]
                                    )
                                ]

                            Loading ->
                                []

                            LoadingSlowly ->
                                [ Loading.icon ]

                            Failed ->
                                [ Loading.error "feed" ]
    )                    , div [ class "col-md-3" ] <|
                        case model.tags of
                            Loaded tags ->
                                [ div [ class "sidebar" ] <|
                                    [ p [] [ text "Popular Tags" ]
                                    , viewTags tags
                                    ]
                                ]

                            Loading ->
                                []

                            LoadingSlowly ->
                                [ Loading.icon ]

                            Failed ->
                                [ Loading.error "tags" ]
                    ]
                ]
            ]
    }
""")



}
