package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmUpperPathTypeRef
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference to a type
 */
class SimpleTypeReference(element: ElmReferenceElement)
    : ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> {
        return ModuleScope(element.elmFile).getVisibleTypes().toTypedArray()
    }

    override fun resolve(): ElmPsiElement? {
        return getVariants().find { it.name == element.referenceName }
    }
}
