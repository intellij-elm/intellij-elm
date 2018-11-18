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
import org.intellij.lang.annotations.Language

class ElmParameterInfoHandlerTest : ElmTestBase() {
    // TODO: At some point, the type annotations on each function should no longer be necessary.
    //       But right now the type inference system needs them.


    // must include Elm stdlib because the type inference code must resolve types like String, Char, Int
    override fun getProjectDescriptor() =
            ElmWithStdlibDescriptor


    // SOLO ARGUMENT


    fun `test function with one arg`() = checkByText("""
f : String -> Int
f x = 42
main = f "foo"{-caret-}
""", "f : String → Int")

    fun `test function with one arg nested in another function call`() = checkByText("""
f : Int -> String
f x = "blah"
g : Char -> Int
g x = 99
main = f (g 'c'{-caret-})
""", "g : Char → Int")

    fun `test higher-order function renders the hint with parens`() = checkByText("""
f : (String -> Char) -> Int
f g = 0
main = f {-caret-}(\s -> 'x')
""", "f : (String → Char) → Int")


    // MULTIPLE ARGUMENTS


    fun `test function with two args, caret on first arg`() = checkByText("""
f : String -> Char -> Int
f x y = 42
main = f "hi"{-caret-}
""", "f : String → Char → Int")

    fun `test function with two args, caret on second arg`() = checkByText("""
f : String -> Char -> Int
f x y = 42
main = f "x" {-caret-}
""", "f : String → Char → Int")

    fun `test function with two args nested in another function call`() = checkByText("""
f : Int -> Int
f x = 42
g : Char -> Bool -> Int
g x y = 99
main = f (g 'x' {-caret-})
""", "g : Char → Bool → Int")


    // UNNAMED FUNCTIONS


    fun `test lambda`() = checkByText("""
main = (\a b -> "done") "hi"{-caret-}
""", "a → b → String")

    fun `test expression`() = checkByText("""
main = ((+) 1) 1{-caret-}
""", "number → number")


    // UTILS


    private fun checkByText(@Language("Elm") code: String, hint: String) {
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
            TestCase.assertNotNull(element)
        } else if (elt != null) {
            throw AssertionFailedError("Unexpected hint found")
        }
    }
}
