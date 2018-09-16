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

package org.elm.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.elm.fileTreeFromText
import org.elm.hasCaretMarker
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language


abstract class ElmCompletionTestBase: ElmTestBase() {

    protected fun doSingleCompletion(@Language("Elm") before: String, @Language("Elm") after: String) {
        check(hasCaretMarker(before) && hasCaretMarker(after)) {
            "Please add `{-caret-}` marker"
        }
        checkByText(before, after) { executeSoloCompletion() }
    }

    protected fun doSingleCompletionMultiFile(@Language("Elm") before: String, @Language("Elm") after: String) {
        fileTreeFromText(before).createAndOpenFileWithCaretMarker()
        executeSoloCompletion()
        myFixture.checkResult(replaceCaretMarker(after.trimIndent()))
    }

    protected fun checkContainsCompletion(text: String, @Language("Elm") code: String) {
        InlineFile(code).withCaret()
        val variants = myFixture.completeBasic()
        when {
            variants == null -> {
                // IntelliJ returns null if there was only one completion possible. In which case, all we need
                // to do is verify that the completion that it performed was the one that was expected.
                // Unfortunately that's a bit hard to express, so we will just do something sloppy and make sure
                // that the expected text got inserted somewhere. What could possibly go wrong? ;-)
                val fullText = myFixture.editor.document.text
                check(fullText.contains(text)) {
                    "Expected completions that contain $text, but it actually completed as something else:\n$fullText"
                }
            }
            variants.isEmpty() ->
                error("Expected completions that contain $text, but got no completions at all")

            variants.none { it.lookupString == text } ->
                error("Expected completions that contain $text, but got ${variants.toList()}")
        }
    }

    protected fun checkNoCompletion(@Language("Elm") code: String) {
        InlineFile(code).withCaret()
        noCompletionCheck()
    }

    protected fun checkNoCompletionWithMultiFile(@Language("Elm") code: String) {
        fileTreeFromText(code).createAndOpenFileWithCaretMarker()
        noCompletionCheck()
    }

    private fun noCompletionCheck() {
        val variants = myFixture.completeBasic()
        checkNotNull(variants) {
            val element = myFixture.file.findElementAt(myFixture.caretOffset - 1)
            "Expected zero completions, but one completion was auto inserted: `${element?.text}`."
        }
        check(variants.isEmpty()) {
            "Expected zero completions, got ${variants.size}."
        }
    }

    protected fun executeSoloCompletion() {
        val variants = myFixture.completeBasic()

        if (variants != null) {
            if (variants.size == 1) {
                // for cases like `frob{-caret-}nicate()`,
                // completion won't be selected automatically.
                myFixture.type('\n')
                return
            }
            fun LookupElement.debug(): String = "$lookupString ($psiElement)"
            error("Expected a single completion, but got ${variants.size}\n"
                    + variants.joinToString("\n") { it.debug() })
        }
    }
}