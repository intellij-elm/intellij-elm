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

package org.elm.ide.search

import com.intellij.psi.PsiElement
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmNamedElement
import org.intellij.lang.annotations.Language


class ElmFindUsagesProviderTest: ElmTestBase() {


    fun `test function parameter usage`() = doTestByText(
"""
foo x =
  --^
    let
        a = x -- : foobar
    in
        x -- : foobar
""")


    fun `test binary operator usage`() = doTestByText(
"""
power a b = List.product (List.repeat b a)
infix right 5 (**) = power
              --^

foo = 2 ** 3 -- : foobar
bar = (**) 2 -- : foobar
""")


    private fun doTestByText(@Language("Elm") code: String) {
        InlineFile(code)
        val source = findElementInEditor<ElmNamedElement>()

        val actual = markersActual(source)
        val expected = markersFrom(code)
        assertEquals(expected.joinToString(COMPARE_SEPARATOR), actual.joinToString(COMPARE_SEPARATOR))
    }

    private fun markersActual(source: ElmNamedElement) =
            myFixture.findUsages(source)
                    .filter { it.element != null }
                    // TODO [kl] implement a UsageTypeProvider and replace "foobar" with the expected usage type
                    // both here and in the test cases themselves.
//                    .map { Pair(it.element?.line ?: -1, RsUsageTypeProvider.getUsageType(it.element).toString()) }
                    .map { Pair(it.element?.line ?: -1, "foobar") }

    private fun markersFrom(text: String) =
            text.split('\n')
                    .withIndex()
                    .filter { it.value.contains(MARKER) }
                    .map { Pair(it.index, it.value.substring(it.value.indexOf(MARKER) + MARKER.length).trim()) }

    private companion object {
        val MARKER = "-- : "
        val COMPARE_SEPARATOR = " | "
    }

    val PsiElement.line: Int? get() = containingFile.viewProvider.document?.getLineNumber(textRange.startOffset)

}

