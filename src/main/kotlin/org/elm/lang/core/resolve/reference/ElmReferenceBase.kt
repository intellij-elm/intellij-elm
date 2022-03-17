package org.elm.lang.core.resolve.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement


abstract class ElmReferenceBase<T : ElmReferenceElement>(element: T)
    : ElmReference, PsiPolyVariantReferenceBase<T>(element) {

    override fun resolve(): ElmNamedElement? = super.resolve() as? ElmNamedElement

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val elements = multiResolve()
        return Array(elements.size) { PsiElementResolveResult(elements[it]) }
    }

    override fun calculateDefaultRangeInElement(): TextRange {
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

    // Equality needs to be defined this way in order for the ResolveCache to work.
    override fun equals(other: Any?): Boolean =
            other is ElmReferenceBase<*> &&
                    element === other.element &&
                    this.javaClass == other.javaClass

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + element.hashCode()
        result = 31 * result + javaClass.hashCode()
        return result
    }
}
