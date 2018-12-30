package org.elm.ide.inspections


class ElmUnusedImportInspectionTest : ElmInspectionsTestBase(ElmUnusedImportInspection()) {

    fun `test with qualified ref`() = checkByFileTree("""
        --@ Main.elm
        <warning descr="Unused import">import Foo</warning>
        import Bar
        main = Bar.bar
        --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()

        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
    """.trimIndent())


    fun `test with unqualified ref, exposing selectively`() = checkByFileTree("""
        --@ Main.elm
        <warning descr="Unused import">import Foo exposing (foo)</warning>
        import Bar exposing (bar)
        main = bar
        --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()

        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
    """.trimIndent())


    fun `test with unqualified ref, exposing all`() = checkByFileTree("""
        --@ Main.elm
        <warning descr="Unused import">import Foo exposing (..)</warning>
        import Bar exposing (..)
        main = bar
        --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()

        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
    """.trimIndent())

}