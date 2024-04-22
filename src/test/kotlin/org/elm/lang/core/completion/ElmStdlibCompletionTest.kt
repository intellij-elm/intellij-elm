package org.elm.lang.core.completion

import org.junit.Test


/**
 * Test completion for values and types built-in to the language,
 * such as List, as well as implicit imports from `elm/core` provided
 * by the compiler.
 */
class ElmStdlibCompletionTest : ElmCompletionTestBase() {

    override fun getProjectDescriptor() =
            ElmWithStdlibDescriptor

    @Test
    fun `test built-in type completion in a type annotation`() =
            checkContainsCompletion("List", "name : Lis{-caret-}")

    @Test
    fun `test implicitly imported type completion in a type annotation`() =
            checkContainsCompletion("Maybe", "x : May{-caret-}")

    @Test
    fun `test implicitly imported union constructor completion`() =
            checkContainsCompletion("Nothing", "x = Noth{-caret-}")

    @Test
    fun `test implicitly exposed type from Basics module completion`() =
            checkContainsCompletion("Order", "f : Ord{-caret-}")

    @Test
    fun `test implicitly exposed union constructor from Basics module completion`() =
            checkContainsCompletion("EQ", "f = compare E{-caret-}")

    @Test
    fun `test implicitly exposed value from Basics module completion`() =
            checkContainsCompletion("compare", "x = comp{-caret-}")

}