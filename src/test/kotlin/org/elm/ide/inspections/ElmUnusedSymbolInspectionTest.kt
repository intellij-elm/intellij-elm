package org.elm.ide.inspections


class ElmUnusedSymbolInspectionTest : ElmInspectionsTestBase(ElmUnusedSymbolInspection()) {


    // FUNCTIONS

    fun `test detects unused functions`() = checkByText("""
        <warning descr="'f' is never used">f</warning> = g
        g = ()
        """.trimIndent())


    fun `test the main function is never marked as unused`() = checkByText("""
        main = ()
        """.trimIndent())


    fun `test the type annotation does not count as usage`() = checkByText("""
        f : ()
        <warning descr="'f' is never used">f</warning> = ()
    """.trimIndent())


    fun `test exposing a function does not count as usage`() = checkByText("""
        module Foo exposing (f)
        <warning descr="'f' is never used">f</warning> = ()
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
        f <warning descr="'x' is never used">x</warning> = ()
        main = f
        """.trimIndent())


    fun `test detects unused lambda parameters`() = checkByText("""
        main = (\<warning descr="'x' is never used">x</warning> -> ())
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
        type Foo = Bar | <warning descr="'Quux' is never used">Quux</warning>
        main : Foo
        main = Bar
        """.trimIndent())


    // MISC

    fun `test detects unused alias when importing a module`() = checkByFileTree("""
        --@ main.elm
        import FooBar as <warning descr="'FB' is never used">FB</warning>
        --^

        --@ FooBar.elm
        module FooBar exposing (..)
        """.trimIndent())

}