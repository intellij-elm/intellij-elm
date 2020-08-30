package org.elm.ide.inspections


class ElmUnusedLeftPipeInspectionTest : ElmInspectionsTestBase(ElmUnusedLeftPipeInspection()) {

    val fixName = ElmUnusedLeftPipeInspection.fixName

    fun `test basic unnecessary left pipe - parens`() = checkByText("""
        f =
            identity <warning descr="'<|' is not necessary"><|</warning>
                (text "hi")
        """.trimIndent())

    fun `test basic unnecessary left pipe - list literal`() = checkByText("""
        f =
            identity <warning descr="'<|' is not necessary"><|</warning>
                [1, 2, 3]
        """.trimIndent())

    fun `test basic unnecessary left pipe - record literal`() = checkByText("""
        f =
            identity <warning descr="'<|' is not necessary"><|</warning>
                { x = 0
                , y = 0
                }
        """.trimIndent())

    fun `test does nothing when left pipe IS necessary`() = checkByText("""
        f =
            identity <| text "hi"
        """.trimIndent())

    fun `test does nothing on right pipe`() = checkByText("""
        f =
            identity |> text "hi"
        """.trimIndent())


    fun `test safe delete`() {
        checkFixByText(fixName, """
    f g h = g <warning descr="'<|' is not necessary"><|{-caret-}</warning> (h "hi")
    """, """
    f g h = g (h "hi")
    """)
    }

    fun `test safe delete when comment follows left pipe`() = checkFixByText(fixName, """
f g h = g <warning descr="'<|' is not necessary"><|{-caret-}</warning> {- comment -} (h "hi")
""", """
f g h = g {- comment -} (h "hi")
""")

    fun `test safe delete when newline follows left pipe`() = checkFixByText(fixName, """
f g h = 
    g <warning descr="'<|' is not necessary"><|{-caret-}</warning>
        (h "hi")
""", """
f g h =
    g
        (h "hi")
""")
}
