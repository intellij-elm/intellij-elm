package org.elm.lang.core.completion

class ElmStdlibCompletionTest: ElmCompletionTestBase() {

    fun testDummy() {}


    // TODO [kl] re-enable built-in type and value tests after I implement [CompletionContributor]
    /*
    fun `test built-in type completion in a type annotation`() = doSingleCompletion(
"""
name : Stri{-caret-}
""", """
name : String{-caret-}
""")


    fun `test built-in value completion`() = doSingleCompletion(
"""
x = Fal{-caret-}
""", """
x = False{-caret-}
""")
    */


    // TODO [kl] re-enable these tests once we can arrange to download Elm Core package
    // and configure a custom [LightProjectDescriptor] that includes Core as standard library.

    // TODO [kl] expand the tests to include additional modules that the Elm compiler
    // includes implicitly.

/*
    fun `test implicitly imported type completion in a type annotation`() = doSingleCompletion(
"""
x : May{-caret-}
""", """
x : Maybe{-caret-}
""")


    fun `test implicitly imported union constructor completion`() = doSingleCompletion(
"""
x = Noth{-caret-}
""", """
x = Nothing{-caret-}
""")


    fun `test implicitly exposed type from Basics module completion`() = doSingleCompletion(
"""
f : Ord{-caret-}
""", """
f : Order{-caret-}
""")

    fun `test implicitly exposed union constructor from Basics module completion`() = doSingleCompletion(
"""
f = compare E{-caret-}
""", """
f = compare EQ{-caret-}
""")


    fun `test implicitly exposed value from Basics module completion`() = doSingleCompletion(
"""
x = comp{-caret-}
""", """
x = compare{-caret-}
""")
*/

}