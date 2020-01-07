package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmUnionVariant
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference to a union constructor
 */
class SimpleUnionConstructorReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolveInner(): ElmNamedElement? {
        val referenceName = element.referenceName
        return getCandidates().find { it.name == referenceName }
    }

    private fun getCandidates(): List<ElmNamedElement> =
            ModuleScope.getVisibleConstructors(element.elmFile).all
                    .filterIsInstance<ElmUnionVariant>()
}
