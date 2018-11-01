/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.ide.hints

import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext
import junit.framework.AssertionFailedError
import junit.framework.TestCase
import org.elm.lang.ElmTestBase

class ElmParameterInfoHandlerTest : ElmTestBase() {
    // TODO: At some point, the type annotations on each function should no longer be necessary.
    //       But right now the type inference system needs them.

    // NO ARGS (VALUES)

    fun `test function with no args`() {
        checkByText("""
            x : Int
            x = 42
            main = x{-caret-}
        """.trimIndent(), "<no arguments>", 0)
    }

    // SOLO ARGUMENT

    fun `test function with one arg`() {
        checkByText("""
            f : String -> Int
            f x = 42
            main = f {-caret-}
        """.trimIndent(), "x: String", 0)
    }

    fun `test function with one arg but caret before args`() {
        checkByText("""
            f : String -> Int
            f x = 42
            main = f{-caret-}
        """.trimIndent(), "<no arguments>", 0)
    }

    fun `test function with one arg nested in another function call`() {
        checkByText("""
            f : Int -> Int
            f x = 42
            g : Char -> Int
            g x = 99
            main = f (g {-caret-})
        """.trimIndent(), "x: Char", 0)
    }

    // MULTIPLE ARGUMENTS

    fun `test function with two args, caret on first arg`() {
        checkByText("""
            f : String -> Char -> Int
            f x y = 42
            main = f {-caret-}
        """.trimIndent(), "x: String, y: Char", 0)
    }

    fun `test function with two args, caret on second arg`() {
        checkByText("""
            f : String -> Char -> Int
            f x y = 42
            main = f "x" {-caret-}
        """.trimIndent(), "x: String, y: Char", 1)
    }

    fun `test function with two args, fully specified, caret on first arg`() {
        checkByText("""
            f : String -> Char -> Int
            f x y = 42
            main = f "x"{-caret-} 'y'
        """.trimIndent(), "x: String, y: Char", 0)
    }

    fun `test function with two args nested in another function call`() {
        checkByText("""
            f : Int -> Int
            f x = 42
            g : Char -> Bool -> Int
            g x y = 99
            main = f (g 'x' {-caret-})
        """.trimIndent(), "y: Bool", 1)
    }

    // UTILS

    private fun checkByText(code: String, hint: String, index: Int) {
        myFixture.configureByText("main.elm", replaceCaretMarker(code))
        val handler = ElmParameterInfoHandler()
        val createContext = MockCreateParameterInfoContext(myFixture.editor, myFixture.file)

        // Check hint
        val elt = handler.findElementForParameterInfo(createContext)
        if (hint.isNotEmpty()) {
            elt ?: throw AssertionFailedError("Hint not found")
            handler.showParameterInfo(elt, createContext)
            val items = createContext.itemsToShow ?: throw AssertionFailedError("Parameters are not shown")
            if (items.isEmpty()) throw AssertionFailedError("Parameters are empty")
            val context = MockParameterInfoUIContext(elt)
            handler.updateUI(items[0] as ElmParametersDescription, context)
            TestCase.assertEquals(hint, handler.hintText)

            // Check parameter index
            val updateContext = MockUpdateParameterInfoContext(myFixture.editor, myFixture.file)
            val element = handler.findElementForUpdatingParameterInfo(updateContext)
                    ?: throw AssertionFailedError("Parameter not found")
            handler.updateParameterInfo(element, updateContext)
            TestCase.assertEquals(index, updateContext.currentParameter)
        } else if (elt != null) {
            throw AssertionFailedError("Unexpected hint found")
        }
    }
}