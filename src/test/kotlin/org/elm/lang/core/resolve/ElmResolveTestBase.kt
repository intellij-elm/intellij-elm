package org.elm.lang.core.resolve

import org.elm.fileTreeFromText
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmNamedElement
import org.intellij.lang.annotations.Language


abstract class ElmResolveTestBase : ElmTestBase() {

    protected fun checkByCode(@Language("Elm") code: String) {
        InlineFile(code)

        val (refElement, data) = findElementAndDataInEditor<ElmReferenceElement>("^")

        if (data == "unresolved") {
            val resolved = refElement.reference.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.reference.resolve()
        check(resolved != null) { "resolve did not find anything"}

        val target = findElementInEditor<ElmNamedElement>("X")
        check(resolved == target) { "resolved as $resolved but expected $target"}
    }

    protected fun stubOnlyResolve(@Language("Elm") code: String) {
        fileTreeFromText(code).createAndOpenFileWithCaretMarker()

        // TODO [kl] re-enable this check once we add stub support
        // this code was copied from the IntelliJ Rust plugin which designed their
        // resolve tests to ensure that resolving an import can be done with just stubs.
        /*
        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })
        */

        val (refElement, resolveFile) = findElementAndDataInEditor<ElmReferenceElement>()

        if (resolveFile == "unresolved") {
            val element = refElement.reference.resolve()
            if (element != null) {
                error("Should not resolve ${refElement.text} to ${element.text}")
            }
            return
        }

        val element = refElement.reference.resolve()
                ?: error("Failed to resolve ${refElement.text}")
        val actualResolveFile = element.containingFile.virtualFile

        if (resolveFile.isEmpty()) {
            error("the marker in the test code must include a target file path")
        } else if (resolveFile.startsWith("...")) {
            // TODO [kl] re-visit this
            // I think the Rust plugin was using this to have a relative reference
            // to a file bundled in the Rust stdlib. This would be like Core for Elm,
            // but I guess it will depend on how we handle project libraries when
            // we get to that point. I think the key thing here is that these stdlib
            // references will not be present as files in the test fixture temp dir.
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
}