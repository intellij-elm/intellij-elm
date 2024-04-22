package org.elm.ide.docs

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase.assertSameLines
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test

import org.junit.Assert.*

abstract class ElmDocumentationProviderTest : ElmTestBase() {
    protected inline fun doTest(
            @Language("Elm") code: String,
            @Language("Html") expected: String,
            block: ElmDocumentationProvider.(PsiElement, PsiElement?) -> String?
    ) {
        addFileToFixture(code)

        val (originalElement, _, offset) = findElementWithDataAndOffsetInEditor<PsiElement>()
        val element = DocumentationManager.getInstance(project)
                .findTargetElement(myFixture.editor, offset, myFixture.file, originalElement)!!

        val actual = ElmDocumentationProvider().block(element, originalElement)?.trim()!!
        assertSameLines(expected.trimIndent(), actual)
    }
}
