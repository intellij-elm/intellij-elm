package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope


class SimpleUnionOrRecordConstructorReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolveInner(): ElmNamedElement? {
        val referenceName = element.referenceName
        return getCandidates().find { it.name == referenceName }
    }

    private fun getCandidates(): List<ElmNamedElement> {
        return ModuleScope.getVisibleConstructors(element.elmFile).all
    }
}
