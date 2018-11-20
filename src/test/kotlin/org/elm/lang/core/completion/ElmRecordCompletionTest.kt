package org.elm.lang.core.completion

class ElmRecordCompletionTest : ElmCompletionTestBase() {


    fun `test access name field from one letter`() = doSingleCompletion(
            """
type alias Foo = { name : String }
f : Foo -> String
f foo =
    foo.n{-caret-}
""", """
type alias Foo = { name : String }
f : Foo -> String
f foo =
    foo.name{-caret-}
""")


    fun `test access name field from blank`() = doSingleCompletion(
            """
type alias Foo = { name : String }
f : Foo -> String
f foo =
    foo.{-caret-}
""", """
type alias Foo = { name : String }
f : Foo -> String
f foo =
    foo.name{-caret-}
""")


    fun `test chained field access is not currently supported`() = checkNoCompletion(
            """
type alias Foo = { name : { first: String, last: String } }

f : Foo -> String
f foo =
    foo.name.fir{-caret-}
""")

}