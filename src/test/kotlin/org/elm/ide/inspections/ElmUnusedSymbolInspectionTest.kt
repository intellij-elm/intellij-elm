package org.elm.ide.inspections


class ElmUnusedSymbolInspectionTest : ElmInspectionsTestBase(ElmUnusedSymbolInspection()) {


    // FUNCTIONS

    fun `test detects unused functions`() = checkByText("""
        <error descr="'f' is never used">f</error> = g
        g = ()
        """.trimIndent())


    fun `test the main function is never marked as unused`() = checkByText("""
        main = ()
        """.trimIndent())


    fun `test the type annotation does not count as usage`() = checkByText("""
        f : ()
        <error descr="'f' is never used">f</error> = ()
    """.trimIndent())


    fun `test exposing a function does not count as usage`() = checkByText("""
        module Foo exposing (f)
        <error descr="'f' is never used">f</error> = ()
    """.trimIndent())


    fun `test but a reference to a function from another file DOES count as usage`() = checkByFileTree("""
        --@ Foo.elm
        module Foo exposing (f)
        f = ()
        --^

        --@ Bar.elm
        import Foo
        g = Foo.f
    """.trimIndent())


    // PARAMETERS

    fun `test detects unused function parameters`() = checkByText("""
        f <error descr="'x' is never used">x</error> = ()
        main = f
        """.trimIndent())


    fun `test detects unused lambda parameters`() = checkByText("""
        main = (\<error descr="'x' is never used">x</error> -> ())
        """.trimIndent())


    // TYPES


    // TODO revisit this in the future: maybe only record aliases should be ignored?
    fun `test type aliases are never marked as unused`() = checkByText("""
        type alias Foo = ()
    """.trimIndent())


    // TODO revisit this in the future: maybe we can check to see if none of the
    //      variant constructors are used, and, if so, then the type can be
    //      considered unused (assuming that there are also no refs to the type
    //      name itself).
    fun `test the union type is never marked as unused`() = checkByText("""
        type Foo = Bar
        main = Bar
        """.trimIndent())


    fun `test detects unused union variant constructor`() = checkByText("""
        type Foo = Bar | <error descr="'Quux' is never used">Quux</error>
        main : Foo
        main = Bar
        """.trimIndent())


    // MISC

    fun `test detects unused alias when importing a module`() = checkByFileTree("""
        --@ main.elm
        import FooBar as <error descr="'FB' is never used">FB</error>
        --^

        --@ FooBar.elm
        module FooBar exposing (..)
        """.trimIndent())

}