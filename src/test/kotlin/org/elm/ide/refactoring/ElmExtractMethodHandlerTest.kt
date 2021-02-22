package org.elm.ide.refactoring

import com.intellij.application.options.CodeStyle
import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.indentStyle
import org.intellij.lang.annotations.Language

class ElmExtractMethodHandlerTest : ElmTestBase() {

    override fun getProjectDescriptor() = ElmWithStdlibDescriptor


    // BASICS


    fun `test creates method from base expression`() = doTest("""
f = 4 + {-caret-}3
""", listOf("3", "4 + 3"), 0, """
f = 4 + fn

fn =
   3
""")

    fun `test uses references from base method`() = doTest("""
f =
   let a = 4
   in 4 + {-caret-}a
""", listOf("a", "4 + a"), 1, """
f =
   let a = 4
   in fn a

fn a =
   4 + a
""")


    fun `test can select alternate expression`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 1, """
f =
    fn

fn =
   4 + 3
""")

    fun `test does not duplicate parameters`() = doTest("""
f =
    let x = 4
    in <selection>x + x</selection>
""", emptyList(), 0, """
f =
    let x = 4
    in fn x

fn x =
   x + x
""")

    fun `test passes locally declared parameters referenced inside parens`() = doTest("""
f =
    let x = 4
    in <selection>(x + x)</selection>
""", emptyList(), 0, """
f =
    let x = 4
    in fn x

fn x =
   (x + x)
""")

    // HELPERS


    private fun doTest(
            @Language("Elm") before: String,
            expressions: List<String>,
            target: Int,
            @Language("Elm") after: String
    ) {
        checkByText(before, after) {
            doExtractMethod(expressions, target)
        }
    }

    private fun doExtractMethod(expressions: List<String>, target: Int) {
        var shownTargetChooser = false
        withMockTargetExpressionChooser(object : ExtractExpressionUi {
            override fun chooseTarget(exprs: List<ElmExpressionTag>): ElmExpressionTag {
                shownTargetChooser = true
                TestCase.assertEquals(exprs.map { it.text }, expressions)
                return exprs[target]
            }
        }) {
            myFixture.performEditorAction("ExtractMethod")
            if (expressions.size > 1 && !shownTargetChooser) {
                error("Didn't show chooser")
            }
        }
    }
}
