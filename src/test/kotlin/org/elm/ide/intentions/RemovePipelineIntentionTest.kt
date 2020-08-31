package org.elm.ide.intentions


class RemovePipelineIntentionTest : ElmIntentionTestBase(RemovePipelineIntention()) {


    fun `test right pipe to no pipe`() = doAvailableTest(
            """
module Foo exposing (list)

list =
    [1, 2, 3, 4] |{-caret-}> List.map times2

times2 m n = m * n
""", """
module Foo exposing (list)

list =
    List.map times2 [1, 2, 3, 4]

times2 m n = m * n
""")


    fun `test preserves comments`() = doAvailableTest(
            """
module Foo exposing (list)

list = 
    -- top-level
    [1, 2, 3, 4] {- List.map 1 -}
        -- List.map 2
        {- List.map 3 -}
        {-caret-}|> List.map times2 -- identity 1
        -- identity 2
        |> identity -- end of line

times2 m n = m * n
""", """
module Foo exposing (list)

list = 
    -- top-level
    (-- identity 1
            -- identity 2
            identity
            ({- List.map 1 -}
        -- List.map 2
        {- List.map 3 -}
        List.map times2
        [1, 2, 3, 4]
        )
            ) -- end of line

times2 m n = m * n
""")

    fun `test remove pipeline example from elm-spa`() = doAvailableTest(
            """
module Foo exposing (value)

value =
    Editor.initEdit session slug
        {-caret-}|> updateWith (Editor (Just slug)) GotEditorMsg model
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
        {-caret-}|> stopPropagationOn "click"
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
        {-caret-}|> Html.button (class classStr :: onClickStopPropagation msg :: attrs)
""", """
module Foo exposing (toggleFavoriteButton)

toggleFavoriteButton classStr msg attrs kids =
    Html.button (class classStr :: onClickStopPropagation msg :: attrs) (i [ class "ion-heart" ] [] :: kids)
""")

    fun `test remove left pipeline`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    floor {-caret-}<| 1.3 + 4
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
        {-caret-}|> List.map
""", """
module Foo exposing (example)

example =
    List.map (\fn -> fn [])
""")


    fun `test preserves indentation as starting point`() = doAvailableTest(
            """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            identity
                (Asset.defaultAvatar
                    -- asdf
                    |>{-caret-} Asset.src
                ) 

        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
""", """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            identity
                ((-- asdf
                    Asset.src
                    Asset.defaultAvatar
                    )
                ) 

        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
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
                    [ div [ class "col-md-9" ]
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
""")

    fun `test preserves comments with ordering`() = doAvailableTest(
            """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            -- user isn't logged in - use guest avatar
            Asset.defaultAvatar
                -- normalizes trailing and leading /'s
                {-caret-}|> normalizeImageUrl
                -- sets the HTML src
                |> Asset.src


        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
""", """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            -- user isn't logged in - use guest avatar
            (-- sets the HTML src
                    Asset.src
                    (-- normalizes trailing and leading /'s
                normalizeImageUrl
                Asset.defaultAvatar
                )
                    )


        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
""")

    fun `test multiline without comments`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    []
        {-caret-}|> List.map
            (identity
                identity
            )
        |> List.map
            (identity
                identity
            )

""", """
module Foo exposing (example)

example =
    List.map
            (identity
                identity
            )
            (List.map
            (identity
                identity
            )
        []
        )

""")

    fun `test preserve comments right next to pipeline`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    -- NotFound.view
    NotFound.view
        {-caret-}|> -- Page.view
           Page.view
            -- viewer
            viewer
            Page.Other

""", """
module Foo exposing (example)

example =
    -- NotFound.view
    (-- Page.view
        Page.view
            -- viewer
            viewer
            Page.Other
        NotFound.view
        )

""")


    fun `test preserve comments right next to pipeline 2`() = doAvailableTest(
            """
module Foo exposing (example)

example =
    -- NotFound.view
    -- Page.view
    NotFound.view
        -- viewer
        {-caret-}|> Page.view viewer Page.Other

""", """
module Foo exposing (example)

example =
    -- NotFound.view
    -- Page.view
    (-- viewer
        Page.view viewer Page.Other
        NotFound.view
        )

""")
}
