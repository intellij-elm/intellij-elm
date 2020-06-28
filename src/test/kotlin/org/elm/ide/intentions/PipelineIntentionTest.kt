package org.elm.ide.intentions


class PipelineIntentionTest : ElmIntentionTestBase(PipelineIntention()) {


    fun `test function call to pipeline`() = doAvailableTest(
            """
module Foo exposing (list)

list = List.ma{-caret-}p times2 [1, 2, 3, 4]

times2 m n = m * n
""", """
module Foo exposing (list)

list = [1, 2, 3, 4] |> List.map times2

times2 m n = m * n
""")

}
