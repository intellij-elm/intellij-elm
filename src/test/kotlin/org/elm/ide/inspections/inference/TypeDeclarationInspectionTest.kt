package org.elm.ide.inspections.inference

import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmTypeDeclarationInspection

class TypeDeclarationInspectionTest : ElmInspectionsTestBase(ElmTypeDeclarationInspection()) {
    fun `test bad self-recursion in type alias`() = checkByText("""
<error descr="Infinite recursion">type alias A = A</error>
""")

    fun `test bad mutual self-recursion in type alias`() = checkByText("""
<error descr="Infinite recursion">type alias A = B</error>
type alias B = A
""")

    fun `test good recursion in through union`() = checkByText("""
type alias Alias = { value : Union }
type Union = Variant Alias
""")

    // https://github.com/klazuka/intellij-elm/issues/188
    fun `test allowed recursion through two aliases`() = checkByText("""
type Foo = Foo Alias1
type alias Alias1 = Alias2
type alias Alias2 = { foo : Foo }
""")

    fun `test too few arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 1 instead.">Foo ()</error>
main = Bar
""")

    fun `test correct number of arguments to type`() = checkByText("""
type Foo a b = Bar
main : Foo () ()
main = Bar
""")

    fun `test too many arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 3 instead.">Foo () () ()</error>
main = Bar
""")

    fun `test too many arguments to type in union variant`() = checkByText("""
type Foo a b = Bar
type Baz = Qux (<error descr="The type expects 2 arguments, but it got 3 instead.">Foo () () ()</error>)
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
