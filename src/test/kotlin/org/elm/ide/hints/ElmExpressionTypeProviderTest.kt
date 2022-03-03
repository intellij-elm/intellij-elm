package org.elm.ide.hints

import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmPsiElement
import org.intellij.lang.annotations.Language

class ElmExpressionTypeProviderTest : ElmTestBase() {


    fun `test binary operator`() {
        checkChoices("""
foo a b = a
infix left 0 (~~) = foo
x = ()
y = x ~~ ()
  --^
""", listOf("x", "x ~~ ()"))
    }


    fun `test parenthesized expressions are not provided as a separate, redundant choice`() {
        checkChoices("""
foo a b = a
infix left 0 (~~) = foo
x0 = ()
x1 = ()
x2 = ()
y = x0 ~~ (x1 ~~ x2)
          --^
""", listOf("x1", "x1 ~~ x2", "x0 ~~ (x1 ~~ x2)"))
    }


    fun `test function call`() {
        checkChoices("""
f x = x
y = ()
z = f (f y)
       --^
""", listOf("y", "f y", "f (f y)"))
    }


    fun `test lists`() {
        checkChoices("""
x = ()
y = [x, x]
   --^
""", listOf("x", "[x, x]"))
    }


    fun `test records`() {
        checkChoices("""
x = ()
y = { foo = x }
          --^
""", listOf("x", "{ foo = x }"))
    }


    fun `test record field access`() {
        checkChoices("""
type alias Foo = { x: { y: () } }
f : Foo -> ()
f foo = foo.x.y
       --^
""", listOf("foo", "foo.x", "foo.x.y"))
    }


    private fun checkChoices(@Language("Elm") str: String, choices: List<String>) {
        val provider = ElmExpressionTypeProvider()

        InlineFile(str)
        val elem = findElementInEditor<ElmPsiElement>()

        TestCase.assertEquals(choices, provider.getExpressionsAt(elem).map { it.text })
    }

}