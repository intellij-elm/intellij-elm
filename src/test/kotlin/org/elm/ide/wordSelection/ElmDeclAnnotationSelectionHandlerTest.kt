package org.elm.ide.wordSelection


class ElmDeclAnnotationSelectionHandlerTest : ElmExtendSelectionTestBase() {


    fun `test extends selection from func body to include type annotation`() = doTestWithTrimmedMargins("""
x = "hi"
f : Int
f = 0<caret>
""",
            """
x = "hi"
f : Int
f = <selection>0<caret></selection>
""",
            """
x = "hi"
f : Int
<selection>f = 0<caret></selection>
""",
            """
x = "hi"
<selection>f : Int
f = 0<caret></selection>
""",
            """
<selection>x = "hi"
f : Int
f = 0<caret></selection>
""")


    fun `test extends selection from type annotation to include func body`() = doTestWithTrimmedMargins("""
x = "hi"
f : Int<caret>
f = 0
""",
            """
x = "hi"
f : <selection>Int<caret></selection>
f = 0
""",
            """
x = "hi"
<selection>f : Int<caret></selection>
f = 0
""",
            """
x = "hi"
<selection>f : Int<caret>
</selection>f = 0
""",
            """
x = "hi"
<selection>f : Int<caret>
f = 0</selection>
""",
            """
<selection>x = "hi"
f : Int<caret>
f = 0</selection>
""")

}