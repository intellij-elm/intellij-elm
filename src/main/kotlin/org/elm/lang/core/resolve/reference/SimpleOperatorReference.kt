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
       return getCandidates()[element.referenceName]
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] filter the variants to just include binary operators
        return getCandidates().all.toTypedArray()
    }

    private fun getCandidates() = ModuleScope.getVisibleValues(element.elmFile)
}
