package org.elm.ide.intentions


class PipelineIntentionTest : ElmIntentionTestBase(PipelineIntention()) {


    fun `test function call to pipeline`() = doAvailableTest(
            """
module Foo exposing (list)

list = List.ma{-caret-}p times2 [1, 2, 3, 4]

times2 m n = m * n
""", """
module Foo exposing (list)

list = [1, 2, 3, 4]
    |> List.map times2

times2 m n = m * n
""")


    fun `test function call to pipeline retains precedence with parens`() = doAvailableTest(
            """
module Foo exposing (exclaimGreeting)

exclaimGreeting =
    greet {-caret-}"John" "Doe"
        ++ "!"

greet first last = first ++ " " ++ last

""", """
module Foo exposing (exclaimGreeting)

exclaimGreeting =
    (
         "Doe"
        |> greet "John"
        )
        ++ "!"

greet first last = first ++ " " ++ last

""")

    fun `test multiple pipes in one chain`() = doAvailableTest(
            """
module Foo exposing (value)

import Html

value = List.singlet{-caret-}on (Html.text (String.fromInt (floor 123.45))) -- this is a comment
""", """
module Foo exposing (value)

import Html

value = 123.45
    |> floor
    |> String.fromInt
    |> Html.text
    |> List.singleton -- this is a comment
""")


    fun `test with nested parentheses`() = doAvailableTest(
            """
module Foo exposing (value)

import Html

value = List.singlet{-caret-}on ((Html.text (String.fromInt (floor 123.45))))
""", """
module Foo exposing (value)

import Html

value = 123.45
    |> floor
    |> String.fromInt
    |> Html.text
    |> List.singleton
""")


    fun `test new thing`() = doAvailableTest(
            """
module Foo exposing (value)


value =
    List.ma{-caret-}p String.fromInt (List.map times2 [ 1, 2, 3, 4 ])

""", """
module Foo exposing (value)


value =
    [ 1, 2, 3, 4 ]
        |> List.map times2
        |> List.map String.fromInt

""")

    fun `test pipe with multiple arguments`() = doAvailableTest(
            """
module Foo exposing (value)

value =
    List.singlet{-caret-}on
        (List.singleton
            (String.fromInt
                (clamp 1 100 42)
            )
        )

""", """
module Foo exposing (value)

value =
    42
        |> clamp 1 100
        |> String.fromInt
        |> List.singleton
        |> List.singleton

""")


    fun `test example from elm-spa-example`() = doAvailableTest(
            """
module Foo exposing (fetch)

fetch : Maybe Cred -> Slug -> Http.Request (Article Full)
fetch maybeCred articleSlug =
    Decode.f{-caret-}ield "article" (fullDecoder maybeCred)
        |> Api.get (Endpoint.article articleSlug) maybeCred

""", """
module Foo exposing (fetch)

fetch : Maybe Cred -> Slug -> Http.Request (Article Full)
fetch maybeCred articleSlug =
    maybeCred
        |> fullDecoder
        |> Decode.field "article"
        |> Api.get (Endpoint.article articleSlug) maybeCred

""")


    fun `test example2 from elm-spa-example`() = doAvailableTest(
            """
module Foo exposing (decoder)

decoder =
    Decode.suc{-caret-}ceed Viewer
        |> custom (Decode.field "image" Avatar.decoder)

""", """
module Foo exposing (decoder)

decoder =
    Viewer
        |> Decode.succeed
        |> custom (Decode.field "image" Avatar.decoder)

""")

    fun `test split pipeline with a list as last argument`() = doAvailableTest(
            """
module Foo exposing (foobar)

foobar =
    iden{-caret-}tity [ negate 1 ]

""", """
module Foo exposing (foobar)

foobar =
    [ negate 1 ]
        |> identity

""")

    fun `test to full pipeline from partial pipeline`() = doAvailableTest(
            """
module Foo exposing (initForm)

initForm =
    Api.g{-caret-}et Endpoint.user (Session.cred session) (Decode.field "user" formDecoder)
            |> Http.send CompletedFormLoad
""", """
module Foo exposing (initForm)

initForm =
    formDecoder
        |> Decode.field "user"
        |> Api.get Endpoint.user (Session.cred session)
        |> Http.send CompletedFormLoad
""")

    fun `test preseves comments`() = doAvailableTest(
            """
module Foo exposing (initForm)

initForm =
    Api.g{-caret-}et Endpoint.user (Session.cred session) (Decode.field "user" formDecoder)
    -- this is a comment
            |> Http.send CompletedFormLoad
""", """
module Foo exposing (initForm)

initForm =
    formDecoder
        |> Decode.field "user"
        |> Api.get Endpoint.user (Session.cred session)
        -- this is a comment
        |> Http.send CompletedFormLoad
""")

    fun `test preseves multiple comments`() = doAvailableTest(
            """
module Foo exposing (initForm)

initForm =
    (Decode.field "user" formDecoder
-- comment 1
    |> Api.get Endpoint.user (Session.cred session)
-- comment 2
    |> H{-caret-}ttp.send CompletedFormLoad
-- comment 3

        )
""", """
module Foo exposing (initForm)

initForm =
    (formDecoder
        |> Decode.field "user"
        -- comment 1
        |> Api.get Endpoint.user (Session.cred session)
        -- comment 2
        |> Http.send CompletedFormLoad
-- comment 3

        )
""")


    fun `test chain is merged into parent pipeline`() = doAvailableTest(
            """
module Foo exposing (initForm)

initForm =
    Profile.upd{-caret-}ate subMsg profile
        |> updateWith (Profile username) GotProfileMsg model
""", """
module Foo exposing (initForm)

initForm =
    profile
        |> Profile.update subMsg
        |> updateWith (Profile username) GotProfileMsg model
""")

    fun `test lambda is wrapped in parens when pipelined`() = doAvailableTest(
            """
module Foo exposing (urlParser)

urlParser =
    Url.Parser.cust{-caret-}om "USERNAME" (\str -> Just (Username str))
""", """
module Foo exposing (urlParser)

urlParser =
    (\str -> Just (Username str))
        |> Url.Parser.custom "USERNAME"
""")

    fun `test not available when already fully piped`() = doUnavailableTest(
            """
module Foo exposing (decoder)

decoder : Decoder Profile
decoder =
    Internals
        |> D{-caret-}ecode.succeed
        |> required "bio" (Decode.nullable Decode.string)
        |> required "image" Avatar.decoder
        |> Decode.map Profile
""")


    fun `test not available when already fully piped with lambda`() = doUnavailableTest(
            """
module Foo exposing (decoder)

decoder : Decoder Profile
decoder =
    Viewer.decoder
        |> Api.{-caret-}viewerChanges (\maybeViewer -> toMsg (fromViewer key maybeViewer))
""")

    fun `test pipeline parts that need parens include it`() = doAvailableTest(
            """
module Foo exposing (value)

value =
    List.m{-caret-}ap (\n -> n * 2) (List.singleton ((modBy <| 2) ((\n -> n * 3) (identity (floor <| (123.45 / (toFloat 2)))))))
""", """
module Foo exposing (value)

value =
    (floor <| (123.45 / (toFloat 2)))
        |> identity
        |> (\n -> n * 3)
        |> (modBy <| 2)
        |> List.singleton
        |> List.map (\n -> n * 2)
""")


    fun `test to pipeline preserves comments above the right parts of the pipeline`() = doAvailableTest(
            """
module Foo exposing (value)

value =
    (-- This comment will apply to the whole expression so it stays up top
     Api.g{-caret-}et (Endpoint.profiles uname)
        maybeCred
        -- Decode.field
        (Decode.field "profile"
            -- decoder
            (decoder
                -- maybeCred
                maybeCred
            )
        )
    )
""", """
module Foo exposing (value)

value =
    (-- This comment will apply to the whole expression so it stays up top
     (
         -- maybeCred
          maybeCred
         -- decoder
         |> decoder
         -- Decode.field
         |> Decode.field "profile"
         |> Api.get (Endpoint.profiles uname) maybeCred
         )
    )
""")


    fun `test preserves comments`() = doAvailableTest(
            """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            identity
                (Asset.src
                    -- uses guest avatar if no avatar is present
                    Asset.defaultA{-caret-}vatar
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
                ((
                    -- uses guest avatar if no avatar is present
                     Asset.defaultAvatar
                    |> Asset.src
                    )
                )

        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
""")

    fun `test preserves comments with ordering`() = doAvailableTest(
            """
module Foo exposing (src)

src : Avatar -> Attribute msg
src (Avatar maybeUrl) =
    case maybeUrl of
        Nothing ->
            -- identity
            iden{-caret-}tity
                -- Asset.src
                (Asset.src
                    -- Asset.defaultAvatar
                    Asset.defaultAvatar
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
            -- identity
            (
                -- Asset.defaultAvatar
                 Asset.defaultAvatar
                -- Asset.src
                |> Asset.src
                |> identity
                )

        Just "" ->
            Asset.src Asset.defaultAvatar

        Just url ->
            Html.Attributes.src url
""")
}
