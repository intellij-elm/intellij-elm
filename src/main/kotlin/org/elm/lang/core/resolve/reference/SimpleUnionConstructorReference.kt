package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmUnionMember
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference to a union constructor
 */
class SimpleUnionConstructorReference(element: ElmReferenceElement): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolve(): ElmNamedElement? =
            getCandidates().find { it.name == element.referenceName }

    private fun getCandidates(): Array<ElmNamedElement> =
            ModuleScope(element.elmFile).getVisibleConstructors()
                    .filter { it is ElmUnionMember }
                    .toTypedArray()

}