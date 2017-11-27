package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmExposedUnionConstructor
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.parentOfType

/**
 * Reference from the name of a union constructor in an exposing list to the definition
 * of the union constructor, which may be in this file or a different file, depending
 * on where the containing union type is defined.
 */
class ExposedUnionConstructorReference(exposedUnionConstructor: ElmExposedUnionConstructor) : ElmReferenceBase<ElmExposedUnionConstructor>(exposedUnionConstructor) {

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] maybe I should move this code into a new UnionTypeScope class for consistency?
        val parent = element.parentOfType<ElmExposedType>() ?: error("bad context")
        val unionType = parent.reference.resolve() ?: return emptyArray()

        if (unionType is ElmTypeDeclaration) {
            return unionType.unionMemberList.toTypedArray()
        } else {
            error("resolved to unexpected element")
        }
    }

    override fun resolve(): PsiElement? {
        return getVariants().firstOrNull { it.name == element.referenceName }
    }
}