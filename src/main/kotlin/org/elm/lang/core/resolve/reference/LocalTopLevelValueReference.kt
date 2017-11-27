package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNameIdentifierOwner
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.resolve.ElmReferenceElement

class LocalTopLevelValueReference(element: ElmReferenceElement): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNameIdentifierOwner> {
        // TODO [kl] handle operator function references
        return element.elmFile.descendantsOfType<ElmFunctionDeclarationLeft>()
                .toTypedArray()
    }

    override fun resolve(): ElmPsiElement? {
        return getVariants().firstOrNull { it.name == element.referenceName }
    }
}