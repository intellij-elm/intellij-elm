package org.elm.ide.inspections

class ElmIncompletePatternInspectionTest : ElmInspectionsTestBase(ElmIncompletePatternInspection()) {

    fun `test no params, no existing branch`() = checkFixByText("Add missing case branches", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar ->

        Baz ->

        Qux ->
""")

    fun `test no params, one existing branch`() = checkFixByText("Add missing case branches", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
        Baz -> ()
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Baz -> ()

        Bar ->

        Qux ->
""")

    fun `test no params, two existing branches`() = checkFixByText("Add missing case branches", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    <error>case{-caret-}</error> it of
        Baz ->
            ()

        Qux ->
            ()
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Baz ->
            ()

        Qux ->
            ()

        Bar ->
""")
}
