package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ImportScope

/**
 * Qualified reference to a union constructor or record constructor
 */
class QualifiedConstructorReference(referenceElement: ElmReferenceElement, val upperCaseQID: ElmUpperCaseQID
) : ElmReferenceBase<ElmReferenceElement>(referenceElement) {

    override fun getVariants(): Array<ElmNamedElement> {
        val qualifierPrefix = upperCaseQID
                .upperCaseIdentifierList
                .dropLast(1)
                .joinToString(".") { it.text }

        return ImportScope.fromQualifierPrefixInModule(qualifierPrefix, element.elmFile)
                ?.getExposedUnionOrRecordConstructors()
                ?.toTypedArray()
                ?: emptyArray()
    }

    override fun resolve(): PsiElement? {
        return getVariants().firstOrNull { it.name == element.referenceName }
    }
}
