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

    fun `test right pipe to no pipe`() = doAvailableTest(
            """
module Foo exposing (list)

list = [1, 2, 3, 4] |> List.ma{-caret-}p times2

times2 m n = m * n
""", """
module Foo exposing (list)

list = (List.map times2 [1, 2, 3, 4])

times2 m n = m * n
""")

    fun `test multiple pipes in one chain`() = doAvailableTest(
            """
module Foo exposing (value)

import Html

value = Html.tex{-caret-}t (String.fromInt 123)
""", """
module Foo exposing (value)

import Html

value = (123
    |> String.fromInt
    |> Html.text

        )
""")

}
