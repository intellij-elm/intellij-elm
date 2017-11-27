package org.elm.ide.annotator

import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

abstract class ElmAnnotatorTestBase : ElmTestBase() {
    protected fun doTest(vararg additionalFilenames: String) {
        myFixture.testHighlighting(fileName, *additionalFilenames)
    }

    protected fun checkInfo(@Language("Elm") text: String) {
        myFixture.configureByText("main.elm", text)
        myFixture.testHighlighting(false, true, false)
    }

    protected fun checkWarnings(@Language("Elm") text: String) {
        myFixture.configureByText("main.elm", text)
        myFixture.testHighlighting(true, false, true)
    }

    protected fun checkErrors(@Language("Elm") text: String) {
        myFixture.configureByText("main.elm", text)
        myFixture.testHighlighting(false, false, false)
    }

    protected fun checkQuickFix(
            fixName: String,
            @Language("Elm") before: String,
            @Language("Elm") after: String
    ) = checkByText(before, after) { applyQuickFix(fixName) }
}
