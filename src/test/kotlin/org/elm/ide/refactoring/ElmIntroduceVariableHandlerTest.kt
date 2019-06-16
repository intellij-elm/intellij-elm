package org.elm.ide.refactoring

import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmExpressionTag
import org.intellij.lang.annotations.Language

class ElmIntroduceVariableHandlerTest : ElmTestBase() {

    override fun getProjectDescriptor() = ElmWithStdlibDescriptor


    // BASICS


    fun `test creates let-in when necessary`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 0, """
f =
    let
        number =
            3
    in
    4 + number
""")


    fun `test can select alternate expression`() = doTest("""
f =
    4 + {-caret-}3
""", listOf("3", "4 + 3"), 1, """
f =
    let
        number =
            4 + 3
    in
    number
""")


    fun `test uses explicit selection when present`() = doTest("""
f =
    <selection>4 + 3</selection>
""", emptyList(), 0, """
f =
    let
        number =
            4 + 3
    in
    number
""")


    fun `test reuses existing let-in`() = doTest("""
f =
    let
        k =
            4
    in
    k + {-caret-}3
""", listOf("3", "k + 3"), 0, """
f =
    let
        k =
            4

        number =
            3
    in
    k + number
""")


    fun `test creates after the last decl in a let-in`() = doTest("""
f =
    let
        k =
            4

        r =
            0
    in
    k + {-caret-}3
""", listOf("3", "k + 3"), 0, """
f =
    let
        k =
            4

        r =
            0

        number =
            3
    in
    k + number
""")


    // EDGE CASES


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
                number =
                    3
            in
            4 + number
    in
    k
""")


    fun `test creates in a let expression body nested in a let expr inner decl`() = doTest("""
f =
    let
        k =
            let
                y =
                    4
            in
            y + {-caret-}3
    in
    k
""", listOf("3", "y + 3"), 0, """
f =
    let
        k =
            let
                y =
                    4

                number =
                    3
            in
            y + number
    in
    k
""")


    fun `test works for very simple functions where the entire body is replaced`() = doTest("""
f =
    {-caret-}3
""", listOf("3"), 0, """
f =
    let
        number =
            3
    in
    number
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
                number =
                    3
            in
            number
""")


    fun `test introduces a let-in within a lambda`() = doTest("""
f =
    \_ ->
        {-caret-}3
""", listOf("3", "\\_ ->\n        3"), 0, """
f =
    \_ ->
        let
            number =
                3
        in
        number
""")


    fun `test extracts if predicate expr cleanly`() = doTest("""
f k =
    if {-caret-}0 == identity k then 1 else 0
""", listOf("0", "0 == identity k", "if 0 == identity k then 1 else 0"), 1, """
f k =
    let
        bool =
            0 == identity k
    in
    if bool then 1 else 0
""")


    // AJ example1 and example3
    fun `test caret anywhere inside a case extracts the entire case`() = doTest("""
f x =
    case x of
        {-caret-}blah -> x
""", listOf("case x of\n        blah -> x"), 0, """
f x =
    let
        a =
            case x of
                blah -> x
    in
    a
""")


    // AJ example2: throws an exception about an invalid offset when trying to do inplace rename
    fun `test extract lambda inside parens`() = doTest("""
g =
    (\extractMe{-caret-} -> ())
""", listOf("""\extractMe -> ()""", """(\extractMe -> ())"""), 0, """
g =
    let
        f =
            \extractMe -> ()
    in
    (f)
""")


    // AJ example4: original implementation was cutting off the record literal
    fun `test extract record literal`() = doTest("""
f =
    { field = 1{-caret-}
    , field2 = 2
    }
""", listOf("1", "{ field = 1\n    , field2 = 2\n    }"), 1, """
f =
    let
        record =
            { field = 1
            , field2 = 2
            }
    in
    record
""")


    // AJ example5: multi-line expression cuts off the '+' and everything after
    fun `test extract multiline binary op expr`() = doTest("""
f =
    1
    +{-caret-}
    2
""", listOf("1\n    +\n    2"), 0, """
f =
    let
        number =
            1
            +
            2
    in
    number
""")


    // AJ example5 alt: multi-line expression cuts off the '+' and everything after
    fun `test extract multiline binary op expr indented`() = doTest("""
f =
    1
        +{-caret-}
        2
""", listOf("1\n        +\n        2"), 0, """
f =
    let
        number =
            1
                +
                2
    in
    number
""")


    // AJ example8 alt: extracting entire let-in generated code mangles previous top-level decl
    fun `test extract entire let-in`() = doTest("""
module Foo exposing (f)

f =
    <selection>let
        x =
            1
    in
    2</selection>
""", listOf("let\n    x = 1\nin\n2"), 0, """
module Foo exposing (f)

f =
    let
        number =
            let
                x =
                    1
            in
            2
    in
    number
""")


    fun `test extend a let with a multi-line expression`() = doTest("""
f =
    let
        _ =
            ()
    in
    [ 0{-caret-}
    , 1
    , 2
    ]
""", listOf("0", "[ 0\n    , 1\n    , 2\n    ]"), 1, """
f =
    let
        _ =
            ()

        list =
            [ 0
            , 1
            , 2
            ]
    in
    list
""")


    fun `test indented multi-line expression`() = doTest("""
f g =
    g
        [ 0{-caret-}
        , 1
        , 2
        ]
""", listOf("0", "[ 0\n        , 1\n        , 2\n        ]", "g\n        [ 0\n        , 1\n        , 2\n        ]"), 1, """
f g =
    let
        list =
            [ 0
            , 1
            , 2
            ]
    in
    g
        list
""")


    fun `test preserve indentation when extracting multi-line expr into an existing let-in`() = doTest("""
f =
    let
        k =
            4
    in
    2{-caret-}
        *
        k
""", listOf("2", "2\n        *\n        k"), 1, """
f =
    let
        k =
            4

        number =
            2
                *
                k
    in
    number
""")


    // NAME SUGGESTIONS


    fun `test suggests based on function call name`() = doTest("""
f =
    selectWidget {-caret-}3

selectWidget w = w
""", listOf("3", "selectWidget 3"), 1, """
f =
    let
        widget =
            selectWidget 3
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
        title =
            model.currentPage.title
    in
    title
""")


    fun `test suggest an alternate name if the default is already taken`() = doTest("""
f number =
    {-caret-}42
""", listOf("42"), 0, """
f number =
    let
        number1 =
            42
    in
    number1
""")


    // HELPERS


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