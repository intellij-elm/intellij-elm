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
(**) a b = a ^ b
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

