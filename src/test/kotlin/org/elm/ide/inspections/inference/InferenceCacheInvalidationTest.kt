package org.elm.ide.inspections.inference

import com.intellij.psi.PsiDocumentManager
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.directChildrenOfType
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.types.InferenceResult
import org.elm.lang.core.types.findInference
import org.intellij.lang.annotations.Language

class InferenceCacheInvalidationTest : ElmTestBase() {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test reinferred only current function after insert in annotated function`() = doTest("1", """
foo: List Int
foo = [ {-caret-} ]
bar = ()
""", "foo")

    fun `test reinferred only current function after remove in annotated function`() = doTest("\b", """
foo: List Int
foo = [ 2{-caret-} ]
bar = ()
""", "foo")

    fun `test reinferred only current function after replace in annotated function`() = doTest("\b3", """
foo: List Int
foo = [ 2{-caret-} ]
bar = ()
""", "foo")

    fun `test reinferred everything on insert in unannotated function`() = doTest("1", """
foo = [ {-caret-} ]
bar = ()
""", "foo", "bar")

    fun `test reinferred everything on type change`() = doTest("a", """
type Foo a = Bar {-caret-}
foo = ()
bar = ()
""", "foo", "bar")

    fun `test reinferred everything on type alias change`() = doTest(", baz: a", """
type alias Foo a = { bar: a{-caret-} }
foo = ()
bar = ()
""", "foo", "bar")

    private fun doTest(type: String, @Language("Elm") code: String, vararg expected: String) {
        InlineFile(code).withCaret()
        val before = collectInference()

        myFixture.type(type)
        PsiDocumentManager.getInstance(project).commitAllDocuments() // process PSI modification events

        val changed = collectInference()
                .filter { before[it.key] !== it.value }
                .keys.toList()

        assertEquals(expected.asList(), changed)
    }

    private fun collectInference(): Map<String, InferenceResult> {
        val directChildrenOfType = myFixture.file.directChildrenOfType<ElmValueDeclaration>()
        return directChildrenOfType
                .associate { it.functionDeclarationLeft!!.name to it.findInference()!! }
    }
}
