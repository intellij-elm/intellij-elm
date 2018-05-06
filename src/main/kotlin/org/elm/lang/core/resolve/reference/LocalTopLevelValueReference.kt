package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * A reference from the left-hand-side of a type annotation to a LOCALLY declared
 * value or function name.
 */
class LocalTopLevelValueReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants() =
            ModuleScope(element.elmFile).getDeclaredValues().toTypedArray()
}