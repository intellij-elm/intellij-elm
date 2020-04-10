package org.elm.ide.inspections


class ElmDuplicateDeclarationInspectionTest : ElmInspectionsTestBase(ElmDuplicateDeclarationInspection()) {
    fun `test no dupes`() = checkByText("""
type Foo = Foo
foo = Foo
""")

    fun `test function dupes`() = checkByText("""
<error descr="Multiple declarations with name 'foo'">foo</error> = ()
<error descr="Multiple declarations with name 'bar'">bar</error> = ()
baz = ()
port <error descr="Multiple declarations with name 'foo'">foo</error> : () -> ()
<error descr="Multiple declarations with name 'bar'">bar</error> = ()
<error descr="Multiple declarations with name 'foo'">foo</error> = ()
""")

    fun `test type dupes`() = checkByText("""
type <error descr="Multiple declarations with name 'Foo'">Foo</error> = Foo
type Bar = Foo
type alias <error descr="Multiple declarations with name 'Foo'">Foo</error> = ()
type alias <error descr="Multiple declarations with name 'Foo'">Foo</error> = ()
""")

    fun `test nested dupe of top level`() = checkByText("""
<error descr="Multiple declarations with name 'foo'">foo</error> = 1
main =
    let
        <error descr="Multiple declarations with name 'foo'">foo</error> = 2
    in
    foo
""")

    fun `test nested dupe of param`() = checkByText("""
main <error descr="Multiple declarations with name 'foo'">foo</error> =
    let
        <error descr="Multiple declarations with name 'foo'">foo</error> = 1
    in
    foo
""")


    fun `test nested dupe of sibling`() = checkByText("""
main =
    let
        <error descr="Multiple declarations with name 'foo'">foo</error> = 1
        <error descr="Multiple declarations with name 'foo'">foo</error> = 2
    in
    foo
""")

    fun `test nested dupe outer`() = checkByText("""
main =
    let
        <error descr="Multiple declarations with name 'foo'">foo</error> = 1
        mid =
            let
               <error descr="Multiple declarations with name 'foo'">foo</error> = 2
            in
            foo
    in
    foo
""")

    fun `test nested dupe parent`() = checkByText("""
main =
    let
        <error descr="Multiple declarations with name 'foo'">foo</error> =
            let
               <error descr="Multiple declarations with name 'foo'">foo</error> = 2
            in
            foo
    in
    foo
""")

    fun `test nested dupe of top-level in destructured assignment`() = checkByText("""
<error descr="Multiple declarations with name 'foo'">foo</error> = 0
main =
    let
        (<error descr="Multiple declarations with name 'foo'">foo</error>, bar) = (1, 2)
    in
    foo
""")

    fun `test no dupes in right-hand-side of destructured assignment`() = checkByText("""
main =
    let
        (x, y) =
            case (0, 0) of
                (a, 0) -> (1, 2)
                (0, a) -> (3, 4)
    in
    x
""")

    fun `test no dupes of imported names`() = checkByFileTree("""
        --@ Main.elm
        import Foo exposing (foo)
        import Bar exposing (..)
        main =
            let
                foo = 0
                bar = 1
            in
            2
          --^

        --@ Foo.elm
        module Foo exposing (..)
        foo = ()

        --@ Bar.elm
        module Bar exposing (..)
        bar = ()
        """.trimIndent())
}
