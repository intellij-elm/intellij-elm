package org.elm.ide.intentions


class RemovePipelineIntentionTest : ElmIntentionTestBase(RemovePipelineIntention()) {


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

}
