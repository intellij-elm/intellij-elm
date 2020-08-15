package org.elm.lang.core.completion

class ElmRecordLiteralCompletionTest : ElmCompletionTestBase() {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test complete field from blank`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo
f =
    { n{-caret-} }
""", """
type alias Foo = { name : String }

f : Foo
f =
    { name = {-caret-} }
""")


    fun `test complete field from one letter`() = doSingleCompletion(
            """
type alias Foo = { name : String, other: String }

f : Foo
f =
    { n{-caret-} }
""", """
type alias Foo = { name : String, other: String }

f : Foo
f =
    { name = {-caret-} }
""")

    fun `test complete single missing field from blank when record type contains two fields`() = doSingleCompletion(
            """
type alias Foo = { name : String, other:String }

f : Foo
f =
    { name="", {-caret-}}
""", """
type alias Foo = { name : String, other:String }

f : Foo
f =
    { name="", other = {-caret-}}
""")

    fun `test no completions when all fields already defined`() = checkNoCompletion(
            """
type alias Foo = { name : String }

f : Foo
f =
    { name = "", n{-caret-} }
""")

    fun `test no completions when no type annotation`() = checkNoCompletion(
            """
f =
    { name = "", n{-caret-} }
""")

    fun `test complete field in record update`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo -> Foo
f foo =
    { foo | n{-caret-} }
""", """
type alias Foo = { name : String }

f : Foo -> Foo
f foo =
    { foo | name = {-caret-} }
""")


    fun `test complete field in root of nested record`() = doSingleCompletion(
            """
type alias Foo = { first : { second: {third: String} } }

f : Foo
f =
    { f{-caret-} }
""", """
type alias Foo = { first : { second: {third: String} } }

f : Foo
f =
    { first = {-caret-} }
""")

    // TODO. Implemented this.
    fun `test complete field in leaf of nested record`() = checkNoCompletion(
            """
type alias Foo = { first : { second: String } }

f : Foo
f =
    { first = { s{-caret-} } }
""")

    fun `test complete field in custom type declaration`() = doSingleCompletion(
            """
type Bar = Baz {name:String}
type alias Foo = { bar : Bar }

f : String -> Foo
f str =
    { bar = Baz { {-caret-} } }
""", """
type Bar = Baz {name:String}
type alias Foo = { bar : Bar }

f : String -> Foo
f str =
    { bar = Baz { name = {-caret-} } }
""")

    fun `test complete field in let block`() = doSingleCompletion(
            """
f =
  let
    f2 : { name : String }
    f2 =
        { n{-caret-} }
  in
  f2
""", """
f =
  let
    f2 : { name : String }
    f2 =
        { name = {-caret-} }
  in
  f2
""")


    fun `test complete field in case expression without type annotation`() = doSingleCompletion(
            """
f b =
  case b of 
    True ->
      { name = "foo" }
    False ->
       { n{-caret-} }
""", """
f b =
  case b of 
    True ->
      { name = "foo" }
    False ->
       { name = {-caret-} }
""")

    fun `test complete missing field from blank with field value set`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo
f =
    { {-caret-} = "" }
""", """
type alias Foo = { name : String }

f : Foo
f =
    { name{-caret-} = "" }
""")

    fun `test complete field from one letter with field value set`() = doSingleCompletion(
            """
type alias Foo = { name : String }

f : Foo
f =
    { n{-caret-} = "" }
""", """
type alias Foo = { name : String }

f : Foo
f =
    { name{-caret-} = "" }
""")

    fun `test complete field in extensible record`() = doSingleCompletion(
            """
type alias Named a = { a | name : String}

f : Named a
f =
    { n{-caret-} }
""", """
type alias Named a = { a | name : String}

f : Named a
f =
    { name = {-caret-} }
""")


    fun `test complete field in custom type declaration nested in incomplete record`() = doSingleCompletion(
            """
type Bar = Baz {name:String}
type Foo = { first: String, second: {third: Bar} }

f: Foo
f =
  { second = { third = Baz { {-caret-} } } }
""", """
type Bar = Baz {name:String}
type Foo = { first: String, second: {third: Bar} }

f: Foo
f =
  { second = { third = Baz { name = {-caret-} } } }
"""
    )

}
