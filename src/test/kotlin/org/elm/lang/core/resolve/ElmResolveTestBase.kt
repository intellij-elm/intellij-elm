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

package org.elm.lang.core.resolve

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmNamedElement
import org.intellij.lang.annotations.Language


abstract class ElmResolveTestBase : ElmTestBase() {

    protected fun checkByCode(@Language("Elm") code: String) {
        InlineFile(code)

        val (ref, data) = findReferenceWithDataInEditor("^")

        if (data == "unresolved") {
            val resolved = ref?.resolve()
            check(resolved == null) {
                "`${ref?.element?.text}` should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        check(ref != null) { "no reference found at caret" }

        val resolved = ref.resolve()
        check(resolved != null) { "resolve did not find anything"}

        val target = findElementInEditor<ElmNamedElement>("X")
        check(resolved == target) { "resolved as $resolved but expected $target"}
    }

    protected fun stubOnlyResolve(@Language("Elm") code: String) {
        val testProject = fileTreeFromText(code).createAndOpenFileWithCaretMarker()

        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })

        val (ref, resolveFile) = findReferenceWithDataInEditor()

        if (resolveFile == "unresolved") {
            val element = ref?.resolve()
            if (element != null) {
                // Note: we cannot log the element text here because it may force a stub to load ASTNode
                // causing the `checkAstNotLoaded` filter above to throw an exception.
                error("Should not resolve ${ref.element.text} to $element")
            }
            return
        }

        check(ref != null) { "no reference found at caret" }

        val element = ref.resolve()
                ?: error("Failed to resolve ${ref.element.text}")
        val actualResolveFile = element.containingFile.virtualFile

        if (resolveFile.isEmpty()) {
            error("the marker in the test code must include a target file path")
        } else if (resolveFile.startsWith("...")) {
            // verify that it can be found in the workspace
            check(actualResolveFile.path.endsWith(resolveFile.drop(3))) {
                "Should resolve to $resolveFile, was ${actualResolveFile.path} instead"
            }
        } else {
            // a reference to another file bundled as a temp file in the fixture
            val expectedResolveFile = myFixture.findFileInTempDir(resolveFile)
                    ?: error("Can't find `$resolveFile` file")

            check(actualResolveFile == expectedResolveFile) {
                "Should resolve to ${expectedResolveFile.path}, was ${actualResolveFile.path} instead"
            }
        }
    }

    protected fun checkMultiResolve(@Language("Elm") code: String) {
        InlineFile(code)
        val ref = findElementInEditor<ElmReferenceElement>().reference
        check(ref.multiResolve().size == 2) {
            "Expected 2 variants, got ${ref.multiResolve()}"
        }
    }
}
