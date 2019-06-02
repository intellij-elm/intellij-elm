package org.elm.ide.refactoring

import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmExpressionTag
import org.intellij.lang.annotations.Language

class ElmIntroduceVariableHandlerTest : ElmTestBase() {

    override fun getProjectDescriptor() = ElmWithStdlibDescriptor


    fun `test creates let-in when necessary`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 0, """
f =
    let
        foo = 3
    in
    4 + foo
""")


    fun `test can select alternate expression`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 1, """
f =
    let
        foo = 4 + 3
    in
    foo
""")


    fun `test reuses existing let-in`() = doTest("""
f =
    let
        x = 4
    in
    x + {-caret-}3
""", listOf("3", "x + 3"), 0, """
f =
    let
        x = 4

        foo = 3
    in
    x + foo
""")


    fun `test creates after the last decl in a let-in`() = doTest("""
f =
    let
        x = 4

        y = 3
    in
    x + {-caret-}3
""", listOf("3", "x + 3"), 0, """
f =
    let
        x = 4

        y = 3

        foo = 3
    in
    x + foo
""")


    fun `test creates a let within a let`() = doTest("""
f =
    let
        x =
            4 + {-caret-}3
    in
    x
""", listOf("3", "4 + 3"), 0, """
f =
    let
        x =
            let
                foo = 3
            in
            4 + foo
    in
    x
""")


    fun `test creates in a let expression body nested in a let expr inner decl`() = doTest("""
f =
    let
        x =
            let
                y = 4
            in
            y + {-caret-}3
    in
    x
""", listOf("3", "y + 3"), 0, """
f =
    let
        x =
            let
                y = 4

                foo = 3
            in
            y + foo
    in
    x
""")


    fun `test works for very simple functions`() = doTest("""
f =
    {-caret-}3
""", listOf("3"), 0, """
f =
    let
        foo = 3
    in
    foo
""")


    fun `test introduces a let-in within a case branch`() = doTest("""
f =
    case () of
        _ ->
            {-caret-}3
""", listOf("3", "case () of\n        _ ->\n            3"), 0, """
f =
    case () of
        _ ->
            let
                foo = 3
            in
            foo
""")


    fun `test introduces a let-in within a lambda`() = doTest("""
f =
    \_ ->
        {-caret-}3
""", listOf("3", "\\_ ->\n        3"), 0, """
f =
    \_ ->
        let
            foo = 3
        in
        foo
""")


    fun `test suggests based on function call name`() = doTest("""
f =
    selectWidget {-caret-}3

selectWidget x = x
""", listOf("3", "selectWidget 3"), 1, """
f =
    let
        widget = selectWidget 3
    in
    widget

selectWidget x = x
""")


    private fun doTest(
            @Language("Elm") before: String,
            expressions: List<String>,
            target: Int,
            @Language("Elm") after: String,
            replaceAll: Boolean = false
    ) {
        checkByText(before, after) {
            doIntroduceVariable(expressions, target, replaceAll)
        }
    }

    private fun doIntroduceVariable(expressions: List<String>, target: Int, replaceAll: Boolean) {
        var shownTargetChooser = false
        withMockTargetExpressionChooser(object : ExtractExpressionUi {
            override fun chooseTarget(exprs: List<ElmExpressionTag>): ElmExpressionTag {
                shownTargetChooser = true
                TestCase.assertEquals(exprs.map { it.text }, expressions)
                return exprs[target]
            }
        }) {
            myFixture.performEditorAction("IntroduceVariable")
            if (expressions.size > 1 && !shownTargetChooser) {
                error("Didn't show chooser")
            }
        }
    }
}