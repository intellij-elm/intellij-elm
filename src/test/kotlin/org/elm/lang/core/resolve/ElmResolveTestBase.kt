package org.elm.lang.core.resolve

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
        if (resolved == null) {
            val multiResolve = refElement.reference.multiResolve()
            check(multiResolve.size != 1)
            if (multiResolve.isEmpty()) {
                fail("Failed to resolve ${refElement.text}")
            } else {
                fail("Failed to resolve ${refElement.text}, multiple variants:\n${multiResolve.joinToString()}")
            }
        }

        val target = findElementInEditor<ElmNamedElement>("X")

        check(resolved == target)
    }

}