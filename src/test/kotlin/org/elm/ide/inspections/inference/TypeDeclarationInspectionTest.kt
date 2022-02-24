package org.elm.ide.inspections.inference

import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmTypeDeclarationInspection
import org.junit.Test

class TypeDeclarationInspectionTest : ElmInspectionsTestBase(ElmTypeDeclarationInspection()) {

    /*
    The self-recursion tests are ignored because starting in IntelliJ 2019.1,
    `getParameterizedCachedValue` is always creating a new value rather than reusing the cache...
    This was an intentional change made by JetBrains so that `CachedValue` no longer
    caches recursive calls. See:
    https://github.com/JetBrains/intellij-community/commit/33c645167507788cdd6e026c7a7bb86938e65e6d

    We relied on this behavior to report the error, so now our only choice is to fork the code
    restoring the original behavior or to just settle for a false negative. We have chosen the latter.

    @Test
    fun `test bad self-recursion in type alias`() = checkByText("""
<error descr="Infinite recursion">type alias A = A</error>
""")

    @Test
    fun `test bad mutual self-recursion in type alias`() = checkByText("""
<error descr="Infinite recursion">type alias A = B</error>
type alias B = A
""")
    */

    @Test
    fun `test good recursion in through union`() = checkByText("""
type alias Alias = { value : Union }
type Union = Variant Alias
""")

    // https://github.com/klazuka/intellij-elm/issues/188
    @Test
    fun `test allowed recursion through two aliases`() = checkByText("""
type Foo = Foo Alias1
type alias Alias1 = Alias2
type alias Alias2 = { foo : Foo }
""")

    @Test
    fun `test too few arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 1 instead.">Foo ()</error>
main = Bar
""")

    @Test
    fun `test correct number of arguments to type`() = checkByText("""
type Foo a b = Bar
main : Foo () ()
main = Bar
""")

    @Test
    fun `test too many arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 3 instead.">Foo () () ()</error>
main = Bar
""")

    @Test
    fun `test too many arguments to type in union variant`() = checkByText("""
type Foo a b = Bar
type Baz = Qux (<error descr="The type expects 2 arguments, but it got 3 instead.">Foo () () ()</error>)
""")

    @Test
    fun `test no arguments to type`() = checkByText("""
type Foo a b = Bar
main : <error descr="The type expects 2 arguments, but it got 0 instead.">Foo</error>
main = Bar
""")

    // List uses a separate code path, so we need tests for it
    @Test
    fun `test too many arguments to List`() = checkByText("""
main : <error descr="The type expects 1 argument, but it got 2 instead.">List () ()</error>
main = []
""")

    @Test
    fun `test correct number of arguments to List`() = checkByText("""
main : List ()
main = []
""")

    @Test
    fun `test too few arguments to List`() = checkByText("""
main : <error descr="The type expects 1 argument, but it got 0 instead.">List</error>
main = []
""")
}
