package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ImportScope

/**
 * Qualified reference to a type
 */
class QualifiedTypeReference(
        element: ElmReferenceElement,
        val upperCaseQID: ElmUpperCaseQID)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolveInner(): ElmNamedElement? =
            getCandidates().find { it.name == element.referenceName }

    private fun getCandidates(): Array<ElmNamedElement> {
        val qualifierPrefix = upperCaseQID.qualifierPrefix
        return ImportScope.fromQualifierPrefixInModule(qualifierPrefix, element.elmFile)
                .flatMap { it.getExposedTypes() }
                .toTypedArray()
    }
}