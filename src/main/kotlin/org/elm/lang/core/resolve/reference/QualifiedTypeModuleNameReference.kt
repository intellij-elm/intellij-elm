package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.psi.elements.ElmUpperPathTypeRef
import org.elm.lang.core.resolve.ElmReferenceElement


/** The module-prefix portion of the qualified reference */
class QualifiedTypeModuleNameReference(
        element: ElmReferenceElement,
        val upperCaseQID: ElmUpperCaseQID
): ElmQualifiedReferenceBase<ElmReferenceElement>(element) {

    override val elementQID: ElmPsiElement
        get() = upperCaseQID
}