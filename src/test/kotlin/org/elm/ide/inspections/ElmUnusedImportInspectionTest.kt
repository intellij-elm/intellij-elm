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


    // TODO we should support this eventually; punting for now
    fun `test unused union types in the exposing list are ignored, for now`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (Bar, Baz)
        main : Baz -> ()
        main _ = ()
        --^

        --@ Foo.elm
        module Foo exposing (Bar, Baz)
        type Bar = BarConstructor
        type Baz = BazConstructor
    """.trimIndent())


    // TODO we should support this eventually; punting for now
    fun `test unused type aliases in the exposing list are ignored, for now`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (Bar, Baz)
        main : Baz -> ()
        main _ = ()
        --^

        --@ Foo.elm
        module Foo exposing (Bar, Baz)
        type alias Bar = ()
        type alias Baz = ()
    """.trimIndent())


// TODO re-enable these 2 tests once we start detecting unused exposed functions/values
//
//    fun `test unused union types in the exposing list are detected`() = checkByFileTree("""
//        --@ Main.elm
//        import Foo exposing (<warning descr="Unused">Bar</warning>, Baz)
//        main : Baz -> ()
//        main _ = ()
//        --^
//
//        --@ Foo.elm
//        module Foo exposing (Bar, Baz)
//        type Bar = BarConstructor
//        type Baz = BazConstructor
//    """.trimIndent())
//
//
//    fun `test unused type aliases in the exposing list are detected`() = checkByFileTree("""
//        --@ Main.elm
//        import Foo exposing (<warning descr="Unused">Bar</warning>, Baz)
//        main : Baz -> ()
//        main _ = ()
//        --^
//
//        --@ Foo.elm
//        module Foo exposing (Bar, Baz)
//        type alias Bar = ()
//        type alias Baz = ()
//    """.trimIndent())


    // TODO [drop 0.18] revisit this. 0.18 allows individual union variants to be exposed, but Elm 0.19
    //                  requires that they be exposed using `(..)`. Once support for 0.18 is dropped,
    //                  implementing this will be simpler.
    fun `test unused union variants in the exposing list are ignored, for now`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (Bar(..), Baz(..))
        main : Baz
        main = BazConstructor
        --^

        --@ Foo.elm
        module Foo exposing (Bar(..), Baz(..))
        type Bar = BarConstructor
        type Baz = BazConstructor
    """.trimIndent())



}