package org.elm.ide.inspections

class TypeDeclarationInspectionTest : ElmInspectionsTestBase(ElmTypeDeclarationInspection()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor
    fun `test bad self-recursion in type alias`() = checkByText("""
<error descr="Infinite recursion">type alias A = A</error>
""")

    fun `test too many arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 1 instead.">Foo ()</error>
main = Bar
""")

    fun `test correct number of arguments to type`() = checkByText("""
type Foo a b = Bar
main : Foo () ()
main = Bar
""")

    fun `test too few arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 3 instead.">Foo () () ()</error>
main = Bar
""")

    fun `test no arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 0 instead.">Foo</error>
main = Bar
""")

    // List uses a separate code path, so we need tests for it
    fun `test too many arguments to List`() = checkByText("""
main : <error descr="The type expects 1 argument, but it got 2 instead.">List () ()</error>
main = []
""")

    fun `test correct number of arguments to List`() = checkByText("""
main : List ()
main = []
""")

    fun `test too few arguments to List`() = checkByText("""
main : <error descr="The type expects 1 argument, but it got 0 instead.">List</error>
main = []
""")
}
