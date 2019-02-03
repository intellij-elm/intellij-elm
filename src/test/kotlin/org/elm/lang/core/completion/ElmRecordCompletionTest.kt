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


    fun `test chained field access`() = doSingleCompletion(
            """
type alias Foo = { name : { first: String } }

f : Foo -> String
f foo =
    foo.name.fir{-caret-}
""", """
type alias Foo = { name : { first: String } }

f : Foo -> String
f foo =
    foo.name.first{-caret-}
""")

    // partial program tests

    fun `test incomplete case expression`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo -> String
f r =
    case r.{-caret-}
""", """
type alias Foo = { name : String }

f : Foo -> String
f r =
    case r.name{-caret-}
""")

    fun `test unresolved reference`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo -> String
f r =
    foo r.{-caret-}
""", """
type alias Foo = { name : String }

f : Foo -> String
f r =
    foo r.name{-caret-}
""")

    fun `test too many arguments`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo -> Int
f r = 1

g : Foo -> Int
g r = f 1 r.{-caret-}
""", """
type alias Foo = { name : String }

f : Foo -> Int
f r = 1

g : Foo -> Int
g r = f 1 r.name{-caret-}
""")

    fun `test incomplete case expression with chained access`() = doSingleCompletion(
            """
type alias Foo = { name : { first: String } }

f : Foo -> String
f r =
    case r.name.fir{-caret-}
""", """
type alias Foo = { name : { first: String } }

f : Foo -> String
f r =
    case r.name.first{-caret-}
""")
}
