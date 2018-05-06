package org.elm.lang.core.resolve.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.offsetIn
import org.elm.lang.core.resolve.ElmReferenceElement


abstract class ElmReferenceBase<T : ElmReferenceElement>(element: T)
    : ElmReference, PsiReferenceBase<T>(element) {

    override fun calculateDefaultRangeInElement(): TextRange {
        // TODO [kl] eventually introduce the reference anchor concept from the
        // Rust plugin, which, will give us more flexibility in how reference
        // ranges are defined. For now, we will just assume that the identifier
        // associated with the named element comprises the reference.
        val nameElement = element.referenceNameElement
        val startOffset = nameElement.offsetIn(element)
        return TextRange(startOffset, startOffset + nameElement.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val factory = ElmPsiFactory(element.project)
        val identifier = element.referenceNameElement
        val newId = when (identifier.elementType) {
            ElmTypes.LOWER_CASE_IDENTIFIER ->
                    factory.createLowerCaseIdentifier(newElementName)

            ElmTypes.UPPER_CASE_IDENTIFIER ->
                    factory.createUpperCaseIdentifier(newElementName)

            ElmTypes.UPPER_CASE_QID ->
                    factory.createUpperCaseQID(newElementName)

            ElmTypes.OPERATOR_IDENTIFIER ->
                    factory.createOperatorIdentifier(newElementName)

            else -> error("Unsupported identifier type for `$newElementName` (${identifier.elementType}")
        }
        identifier.replace(newId)
        return element
    }

    // Equality needs to be defined this way in order for the ResolveCache to work
    override fun equals(other: Any?): Boolean =
            other is ElmReferenceBase<*> && element === other.element

    override fun hashCode(): Int =
            element.hashCode()

    /**
     * Default implementation of resolve should be good enough in most cases assuming
     * that the subclass implements [getVariants] sensibly.
     */
    override fun resolve(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }
}