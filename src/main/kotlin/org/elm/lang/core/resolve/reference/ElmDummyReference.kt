package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement

/**
 * A reference that always fails to resolve.
 *
 * This is a workaround for allowing ElmReferenceElements to conditionally vend a PsiReference.
 * The unresolved reference annotator will not mark these refs as an error.
 */
class ElmDummyReference(elem: ElmReferenceElement) : ElmReferenceBase<ElmReferenceElement>(elem) {
    override fun resolve(): ElmNamedElement? = null
    override fun getVariants(): Array<ElmNamedElement> = emptyArray()
}