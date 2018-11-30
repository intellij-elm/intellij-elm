package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope


class SimpleUnionOrRecordConstructorReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolveInner(): ElmNamedElement? =
            getCandidates().find { it.name == element.referenceName }

    private fun getCandidates(): Array<ElmNamedElement> {
        return ModuleScope(element.elmFile).getVisibleConstructors().all.toTypedArray()
    }

}