package org.elm.ide.inspections


class ElmDuplicateDeclarationInspectionTest : ElmInspectionsTestBase(ElmDuplicateDeclarationInspection()) {
    fun `test no dupes`() = checkByText("""
type Foo = Foo
foo = Foo
""")

    fun `test function dupes`() = checkByText("""
<error descr="Multiple declarations with name foo">foo</error> = ()
<error descr="Multiple declarations with name bar">bar</error> = ()
baz = ()
port <error descr="Multiple declarations with name foo">foo</error> : () -> ()
<error descr="Multiple declarations with name bar">bar</error> = ()
<error descr="Multiple declarations with name foo">foo</error> = ()
""")

    fun `test type dupes`() = checkByText("""
type <error descr="Multiple declarations with name Foo">Foo</error> = Foo
type Bar = Foo
type alias <error descr="Multiple declarations with name Foo">Foo</error> = ()
type alias <error descr="Multiple declarations with name Foo">Foo</error> = ()
""")
}
