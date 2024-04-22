package org.elm.ide.docs

import com.intellij.psi.PsiManager
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmNamedElement
import org.intellij.lang.annotations.Language
import org.junit.Test

class ElmResolveLinkTest : ElmTestBase(){
    @Test
    fun `test type`() = doTest(
"""
type Foo = Bar
   --X

foo : Foo
foo = 0
--^
""", "Foo")

    @Test
    fun `test type alias`() = doTest(
            """
type alias Foo = Int
         --X

foo : Foo
foo = 0
--^
""", "Foo")

    private fun doTest(@Language("Elm") code: String, link: String) {
        addFileToFixture(code)
        val context = findElementInEditor<ElmNamedElement>("^")
        val expectedElement = findElementInEditor<ElmNamedElement>("X")
        val actualElement = ElmDocumentationProvider()
                .getDocumentationElementForLink(PsiManager.getInstance(project), link, context)
        assertEquals(expectedElement, actualElement)
    }
}
