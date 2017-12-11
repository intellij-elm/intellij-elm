package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference to a binary operator
 */
class SimpleOperatorReference(element: ElmReferenceElement)
    : ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] filter the variants to just include binary operators
        return ModuleScope(element.elmFile).getVisibleValues()
                .toTypedArray()
    }
}
