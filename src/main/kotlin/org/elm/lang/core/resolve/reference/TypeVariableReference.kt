package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.resolve.ElmReferenceElement

/**
 * Reference to a type variable
 *
 * The type variable is defined on the left-hand side of a union type
 * or type alias declaration.
 *
 * e.g. the `a` in `type alias User a = { a | name : String }
 *
 * **NOTE** that the reference may not resolve in cases where the type variable
 * is "unbound".
 *
 * e.g. the `a` in `foo : { a | name : String } -> String`
 */
class TypeVariableReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] simplify this once AJ's interface work is complete
        val decl = element.ancestors.firstOrNull { it is ElmTypeAliasDeclaration || it is ElmTypeDeclaration }

        return when (decl) {
            is ElmTypeAliasDeclaration -> decl.lowerTypeNameList
            is ElmTypeDeclaration -> decl.lowerTypeNameList
            else -> emptyList()
        }.toTypedArray()
    }

    override fun resolveInner(): ElmNamedElement? =
            variants.find { it.name == element.referenceName }

}
