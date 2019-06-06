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
        x = 3
    in
    4 + x
""")


    fun `test can select alternate expression`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 1, """
f =
    let
        x = 4 + 3
    in
    x
""")


    fun `test reuses existing let-in`() = doTest("""
f =
    let
        k = 4
    in
    k + {-caret-}3
""", listOf("3", "k + 3"), 0, """
f =
    let
        k = 4

        x = 3
    in
    k + x
""")


    fun `test creates after the last decl in a let-in`() = doTest("""
f =
    let
        k = 4

        r = 0
    in
    k + {-caret-}3
""", listOf("3", "k + 3"), 0, """
f =
    let
        k = 4

        r = 0

        x = 3
    in
    k + x
""")


    fun `test creates a let within a let`() = doTest("""
f =
    let
        k =
            4 + {-caret-}3
    in
    k
""", listOf("3", "4 + 3"), 0, """
f =
    let
        k =
            let
                x = 3
            in
            4 + x
    in
    k
""")


    fun `test creates in a let expression body nested in a let expr inner decl`() = doTest("""
f =
    let
        k =
            let
                y = 4
            in
            y + {-caret-}3
    in
    k
""", listOf("3", "y + 3"), 0, """
f =
    let
        k =
            let
                y = 4

                x = 3
            in
            y + x
    in
    k
""")


    fun `test works for very simple functions`() = doTest("""
f =
    {-caret-}3
""", listOf("3"), 0, """
f =
    let
        x = 3
    in
    x
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
                x = 3
            in
            x
""")


    fun `test introduces a let-in within a lambda`() = doTest("""
f =
    \_ ->
        {-caret-}3
""", listOf("3", "\\_ ->\n        3"), 0, """
f =
    \_ ->
        let
            x = 3
        in
        x
""")


    fun `test suggests based on function call name`() = doTest("""
f =
    selectWidget {-caret-}3

selectWidget w = w
""", listOf("3", "selectWidget 3"), 1, """
f =
    let
        widget = selectWidget 3
    in
    widget

selectWidget w = w
""")


    fun `test suggests based on end of field access chain`() = doTest("""
f model =
    {-caret-}model.currentPage.title
""", listOf("model", "model.currentPage", "model.currentPage.title"), 2, """
f model =
    let
        title = model.currentPage.title
    in
    title
""")


    fun `test suggest an alternate name if the default is already taken`() = doTest("""
f p =
    let
        x = 0
    in
    x + {-caret-}p.x
""", listOf("p", "p.x", "x + p.x"), 1, """
f p =
    let
        x = 0

        x1 = p.x
    in
    x + x1
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