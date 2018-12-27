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

package org.elm.ide.wordSelection

import com.intellij.codeInsight.editorActions.SelectWordHandler
import com.intellij.ide.DataManager
import org.elm.lang.ElmTestBase

abstract class ElmExtendSelectionTestBase : ElmTestBase() {

    fun doTest(before: String, vararg after: String) {
        myFixture.configureByText("main.elm", before)
        val action = SelectWordHandler(null)
        val dataContext = DataManager.getInstance().getDataContext(myFixture.editor.component)
        for (text in after) {
            action.execute(myFixture.editor, null, dataContext)
            myFixture.checkResult(text, false)
        }
    }

    fun doTestWithTrimmedMargins(before: String, vararg after: String) {
        doTest(before.trimMargin(), *after.map { it.trimMargin() }.toTypedArray())
    }
}
