package org.elm.ide.inspections


class ElmUnusedImportInspectionTest : ElmInspectionsTestBase(ElmUnusedImportInspection()) {


    // TEST UNNECESSARY IMPORTS

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

    fun `test with module alias`() = checkByFileTree("""
        --@ Main.elm
        import Foo <warning descr="Unused alias">as F</warning> exposing (foo)
        main = foo
        --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()
    """.trimIndent())

    fun `test ignores module aliases which are actively used (value)`() = checkByFileTree("""
        --@ Main.elm
        import Foo as F
        main = F.foo
        --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()
    """.trimIndent())

    fun `test ignores module aliases which are actively used (type)`() = checkByFileTree("""
        --@ Main.elm
        import Foo as F
        main : F.Bar
        main = ()
        --^

        --@ Foo.elm
        module Foo exposing (..)
        type alias Bar = ()
    """.trimIndent())

    // https://github.com/klazuka/intellij-elm/issues/197
    fun `test issue 197 record update syntax also counts as usage of an exposed name`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (defaultFoo)
        main = { defaultFoo | bar = () }
        --^

        --@ Foo.elm
        module Foo exposing (..)
        defaultFoo = { bar = () }
    """.trimIndent())


    // TEST UNNECESSARY ITEMS IN THE EXPOSING LIST

    fun `test unused functions in the exposing list are detected`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (<warning descr="'f0' is exposed but unused">f0</warning>, f1)
        main = f1
        --^

        --@ Foo.elm
        module Foo exposing (..)
        f0 = ()
        f1 = ()
    """.trimIndent())


    fun `test imports of Elm 19 kernel JS modules are ignored`() = checkByText("""
        import Elm.Kernel.Dom{-caret-}
    """.trimIndent())


    // TEST OPTIMIZE IMPORTS

    fun `test optimize imports basic`() = checkFixByFileTree("Optimize imports", """
        --@ Main.elm
        module Main exposing (..)
        <warning descr="Unused import">import Foo</warning>{-caret-}
        import Bar
        <warning descr="Unused import">import Quux</warning>
        main = Bar.bar
        --@ Foo.elm
        module Foo exposing (..)
        foo = ()
        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
        --@ Quux.elm
        module Quux exposing (..)
        quux = ()
    """.trimIndent(), """
        module Main exposing (..)
        import Bar
        main = Bar.bar
    """.trimIndent())


    fun `test optimize imports preserves adjacent line comments`() = checkFixByFileTree("Optimize imports", """
        --@ Main.elm
        module Main exposing (..)
        -- this comment should be preserved
        <warning descr="Unused import">import Foo</warning>{-caret-}
        -- this comment should also be preserved
        import Bar
        main = Bar.bar
        --@ Foo.elm
        module Foo exposing (..)
        foo = ()
        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
    """.trimIndent(), """
        module Main exposing (..)
        -- this comment should be preserved
        -- this comment should also be preserved
        import Bar
        main = Bar.bar
    """.trimIndent())


    fun `test optimize imports also cleans-up the exposing list`() = checkFixByFileTree("Optimize imports", """
        --@ Main.elm
        module Main exposing (..)
        <warning descr="Unused import">import Foo</warning>{-caret-}
        import Bar exposing (<warning descr="'b0' is exposed but unused">b0</warning>)
        import Quux exposing (<warning descr="'q0' is exposed but unused">q0</warning>, q1)
        main = Bar.b1 q1
        --@ Foo.elm
        module Foo exposing (..)
        foo = ()
        --@ Bar.elm
        module Bar exposing (..)
        b0 = ()
        b1 = ()
        --@ Quux.elm
        module Quux exposing (..)
        q0 = ()
        q1 = ()
    """.trimIndent(), """
        module Main exposing (..)
        import Bar
        import Quux exposing (q1)
        main = Bar.b1 q1
    """.trimIndent())

}
