package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * A reference to a value or a function name declared at the top level of the file containing [element]
 */
class LocalTopLevelValueReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants() =
            ModuleScope.getDeclaredValues(element.elmFile).toTypedArray()
}