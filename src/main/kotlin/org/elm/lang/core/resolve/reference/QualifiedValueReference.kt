package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmValueQID
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.QualifiedImportScope


/**
 * Qualified reference to a value in an expression scope
 */
class QualifiedValueReference(element: ElmReferenceElement, valueQID: ElmValueQID)
    : ElmReferenceCached<ElmReferenceElement>(element), QualifiedReference {

    override fun getVariants(): Array<ElmNamedElement> = emptyArray()

    override fun resolveInner(): ElmNamedElement? {
        return QualifiedImportScope(qualifierPrefix, element.elmFile)
                .getExposedValue(nameWithoutQualifier)
    }

    override val qualifierPrefix = valueQID.qualifierPrefix
    override val nameWithoutQualifier = element.referenceName
}
