package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.QualifiedImportScope

/**
 * Qualified reference to a union constructor or record constructor
 */
class QualifiedConstructorReference(referenceElement: ElmReferenceElement, val upperCaseQID: ElmUpperCaseQID
) : ElmReferenceCached<ElmReferenceElement>(referenceElement), QualifiedReference {

    override fun getVariants(): Array<ElmNamedElement> = emptyArray()

    override fun resolveInner(): ElmNamedElement? {
        // TODO [kl] depending on context, we may need to restrict the variants to just union constructors
        return QualifiedImportScope(qualifierPrefix, element.elmFile)
                .getExposedConstructor(nameWithoutQualifier)
    }

    override val qualifierPrefix = upperCaseQID.qualifierPrefix
    override val nameWithoutQualifier = element.referenceName
}
