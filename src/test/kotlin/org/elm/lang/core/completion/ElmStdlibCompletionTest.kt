package org.elm.lang.core.completion

/**
 * Test completion for values and types built-in to the language,
 * such as List, as well as implicit imports from `elm/core` provided
 * by the compiler.
 */
class ElmStdlibCompletionTest : ElmCompletionTestBase() {

    override fun getProjectDescriptor() =
            ElmWithStdlibDescriptor

    fun `test built-in type completion in a type annotation`() =
            checkContainsCompletion("List", "name : Lis{-caret-}")

    fun `test implicitly imported type completion in a type annotation`() =
            checkContainsCompletion("Maybe", "x : May{-caret-}")

    fun `test implicitly imported union constructor completion`() =
            checkContainsCompletion("Nothing", "x = Noth{-caret-}")

    fun `test implicitly exposed type from Basics module completion`() =
            checkContainsCompletion("Order", "f : Ord{-caret-}")

    fun `test implicitly exposed union constructor from Basics module completion`() =
            checkContainsCompletion("EQ", "f = compare E{-caret-}")

    fun `test implicitly exposed value from Basics module completion`() =
            checkContainsCompletion("compare", "x = comp{-caret-}")

}