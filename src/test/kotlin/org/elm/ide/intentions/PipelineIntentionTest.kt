package org.elm.ide.intentions


class PipelineIntentionTest : ElmIntentionTestBase(PipelineIntention()) {


    fun `test function call to pipeline`() = doAvailableTest(
            """
module Foo exposing (list)

list = List.ma{-caret-}p times2 [1, 2, 3, 4]

times2 m n = m * n
""", """
module Foo exposing (list)

list = ([1, 2, 3, 4]
    |> List.map times2

        )

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
    ("Doe"
    |> greet "John"

        )
        ++ "!"

greet first last = first ++ " " ++ last

""")

    fun `test multiple pipes in one chain`() = doAvailableTest(
            """
module Foo exposing (value)

import Html

value = List.singlet{-caret-}on (Html.text (String.fromInt (floor 123.45)))
""", """
module Foo exposing (value)

import Html

value = (123.45
    |> floor
    |> String.fromInt
    |> Html.text
    |> List.singleton

        )
""")


    fun `test with nested parentheses`() = doAvailableTest(
            """
module Foo exposing (value)

import Html

value = List.singlet{-caret-}on ((Html.text (String.fromInt (floor 123.45))))
""", """
module Foo exposing (value)

import Html

value = (123.45
    |> floor
    |> String.fromInt
    |> Html.text
    |> List.singleton

        )
""")


    fun `test new thing`() = doAvailableTest(
            """
module Foo exposing (value)


value =
    List.ma{-caret-}p String.fromInt (List.map times2 [ 1, 2, 3, 4 ])

""", """
module Foo exposing (value)


value =
    ([ 1, 2, 3, 4 ]
    |> List.map times2
    |> List.map String.fromInt

        )

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
    (42
    |> clamp 1 100
    |> String.fromInt
    |> List.singleton
    |> List.singleton

        )

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
    (maybeCred
    |> fullDecoder
    |> Decode.field "article"
    |> Api.get (Endpoint.article articleSlug) maybeCred

        )

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
    (Viewer
    |> Decode.succeed
    |> custom (Decode.field "image" Avatar.decoder)

        )

""")

    fun `test split pipeline with a list as last argument`() = doAvailableTest(
            """
module Foo exposing (foobar)

foobar =
    iden{-caret-}tity [ negate 1 ]

""", """
module Foo exposing (foobar)

foobar =
    ([ negate 1 ]
    |> identity

        )

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
    (formDecoder
    |> Decode.field "user"
    |> Api.get Endpoint.user (Session.cred session)
    |> Http.send CompletedFormLoad

        )
""")

}
