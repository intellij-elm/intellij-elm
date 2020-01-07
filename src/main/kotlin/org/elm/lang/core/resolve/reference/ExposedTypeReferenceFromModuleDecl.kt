package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference from the name of a type in the module's exposing list to its definition
 * in the same file.
 */
class ExposedTypeReferenceFromModuleDecl(exposedType: ElmExposedType)
    : ElmReferenceCached<ElmExposedType>(exposedType) {

    override fun resolveInner(): ElmNamedElement? {
        val referenceName = element.referenceName
        return variants.find { it.name == referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        return ModuleScope.getDeclaredTypes(element.elmFile).toTypedArray()
    }

}
