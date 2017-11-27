package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmUnionMember
import org.elm.lang.core.resolve.ElmReferenceElement

/**
 * Reference to a union constructor
 */
class SimpleUnionConstructorReference(element: ElmReferenceElement): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNameIdentifierOwner> =
            // TODO [kl] use module scope to at least get the declaration (should we also include visible?)
            element.elmFile.descendantsOfType<ElmUnionMember>().toTypedArray()

    override fun resolve(): ElmPsiElement? =
            getVariants().firstOrNull { it.name == element.referenceName }
}