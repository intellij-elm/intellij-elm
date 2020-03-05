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
}
