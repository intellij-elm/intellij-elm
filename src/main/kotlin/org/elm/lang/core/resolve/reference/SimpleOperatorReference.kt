package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference to a binary operator
 */
class SimpleOperatorReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun resolveInner(): ElmNamedElement? {
        val referenceName = element.referenceName
        return variants.find { it.name == referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] filter the variants to just include binary operators
        return ModuleScope.getVisibleValues(element.elmFile).all.toTypedArray()
    }
}
