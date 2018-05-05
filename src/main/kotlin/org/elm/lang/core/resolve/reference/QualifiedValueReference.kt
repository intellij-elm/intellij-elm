package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmValueQID
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ImportScope


/**
 * Qualified reference to a value in an expression scope
 */
class QualifiedValueReference(element: ElmReferenceElement, val valueQID: ElmValueQID
): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolve(): ElmNamedElement? =
            getCandidates().find { it.name == element.referenceName }

    private fun getCandidates(): Array<ElmNamedElement> {
        val qualifierPrefix = valueQID.qualifierPrefix
        return ImportScope.fromQualifierPrefixInModule(qualifierPrefix, element.elmFile)
                ?.getExposedValues()
                ?.toTypedArray()
                ?: emptyArray()
    }
}