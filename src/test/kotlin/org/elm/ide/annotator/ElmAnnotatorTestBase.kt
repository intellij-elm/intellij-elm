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

package org.elm.ide.annotator

import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

abstract class ElmAnnotatorTestBase : ElmTestBase() {

    override val dataPath: String
        get() = "org/elm/ide/annotator/fixtures"

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
