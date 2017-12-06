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
        checkNotNull(variants) {
            "Expected completions that contain $text, but no completions found"
        }
        variants.filter { it.lookupString == text }.forEach { return }
        error("Expected completions that contain $text, but got ${variants.toList()}")
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