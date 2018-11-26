package org.elm.lang.core.completion

class ElmRecordCompletionTest : ElmCompletionTestBase() {


    fun `test getName from one letter`() = checkContainsCompletion("name",
            """
type alias Foo =
    { name : String
    , age : Int
    }

getName : Foo -> String
getName foo =
    foo.n{-caret-}
""")


    fun `test getName from blank`() = checkContainsCompletion("name",
            """
type alias Foo =
    { name : String
    , age : Int
    }

getName : Foo -> String
getName foo =
    foo.{-caret-}
""")


    fun `test chained field access is not currently supported`() = checkNoCompletion(
            """
type alias Foo =
    { name : { first: String, last: String }
    , age : Int
    }

getName : Foo -> String
getName foo =
    foo.name.fir{-caret-}
""")
}