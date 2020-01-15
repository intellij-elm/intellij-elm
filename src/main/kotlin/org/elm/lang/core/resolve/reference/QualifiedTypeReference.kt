package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.QualifiedImportScope

/**
 * Qualified reference to a type
 */
class QualifiedTypeReference(
        element: ElmReferenceElement,
        override val qualifierPrefix: String
) : ElmReferenceCached<ElmReferenceElement>(element), QualifiedReference {

    override fun getVariants(): Array<ElmNamedElement> = emptyArray()

    override fun resolveInner(): ElmNamedElement? {
        return QualifiedImportScope(qualifierPrefix, element.elmFile)
                .getExposedType(nameWithoutQualifier)
    }

    override val nameWithoutQualifier = element.referenceName
}
