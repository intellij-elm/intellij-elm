package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.resolve.reference.ElmReferenceBase
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope


class SimpleUnionOrRecordConstructorReference(element: ElmReferenceElement): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> {
        return ModuleScope(element.elmFile).getVisibleUnionOrRecordConstructors().toTypedArray()
    }

    override fun resolve(): ElmPsiElement? =
            getVariants().firstOrNull { it.name == element.referenceName }
}